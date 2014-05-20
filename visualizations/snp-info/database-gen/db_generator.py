#!/usr/bin/python 

from wikitools import wiki, category
import os
import json
import re
import urllib2
import urllib
from bs4 import BeautifulSoup
from bs4 import Comment
import sys
import csv
import sqlite3
from plumbum.cmd import gunzip

def get_snpedia_snp_names():
    # use local cache if possible
    if os.path.isfile('snpcache.json'):
        snpedia = json.loads(open('snpcache.json').read())
    # otherwise get them from snpedia and create the cache
    else:
        print 'creating cache for snps in snpedia ...'
        site = wiki.Wiki('http://bots.snpedia.com/api.php')
        snps = category.Category(site, 'Is_a_snp')
        snpedia = []
        for article in snps.getAllMembersGen(namespaces=[0]):
            snpedia.append(article.title.lower())
        open('snpcache.json', 'w').write(json.dumps(snpedia))
    return snpedia

def download_and_add_snpedia_data(database, max_entries):
    print 'downloading snpedia pages and generating text html ...'

    conn = sqlite3.connect(database) 
    c = conn.cursor()

    c.execute('CREATE TABLE snpedia (rs text, html text, strand_info text)')

    # get the names of all snps in snpedia
    snpedia = get_snpedia_snp_names()
    # filter out snp names that aren't rs numbers
    rsnumbers = rs_filter(snpedia)
    
    # limit size
    if max_entries > 0:
        rsnumbers = rsnumbers[:max_entries]

    for idx, rs in enumerate(rsnumbers):
        print 'processing snp {0} out of {1} ...'.format(idx + 1, len(rsnumbers))

        while True:
            try:
                query = 'http://www.snpedia.com/index.php/'+rs+'?action=render'
                response = urllib2.urlopen(query)
                html = response.read()
            except urllib2.URLError:
                print "error downloading page, trying again ..."
                continue
            break

        c.execute('INSERT INTO snpedia VALUES (?, ?, ?)', (rs, extract_snpedia_text(html), extract_snpedia_strand_info(html)))

    conn.commit()
    conn.close()


def rs_filter(snp_names):
    return [snp for snp in snp_names if re.match(r'rs\d+', snp)]

def extract_snpedia_strand_info(page):
    # the strand info is contained between these unique tags
    regex = r'title="Orientation">Orientation</a></td><td>(plus|minus)</td></tr>'
    m = re.findall(regex, page, re.MULTILINE)

    if(m):
        # plus or minus
        return m[0]
    else:
        return "undefined"

def extract_snpedia_text(html_page):

    soup = BeautifulSoup(html_page)

    # remove comments
    for comment in soup.findAll(text=lambda text:isinstance(text, Comment)):
        comment.extract()

    # remove unwanted tags 
    for t in soup.body.find_all(recursive=False):
        if t in soup.body.find_all('table') or \
                t in soup.body.find_all('div'):
            t.extract()

    # remove dead image links
    for t in soup.body.find_all('a'):
        if t.get('class') and 'image' in t.get('class'):
            t.extract()

    return soup.body.prettify(formatter='html').encode('utf-8')

# def generate_snpedia_charts(rsnumbers):
#     print 'generating snpedia charts ...'
#
#     for idx, rs in enumerate(rsnumbers):
#         print 'processing snp {0} out of {1} ...'.format(idx + 1, len(rsnumbers))
#
#         page = open('snp_db/'+rs+'/snpedia_page.html').read()
#
#
#         m = re.search(url_regex, page)
#
#         if m:
#             soup = BeautifulSoup(m.group(1))
#             urllib.urlretrieve(soup.string, 'snp_db/'+rs+'/snpedia_chart.png')

# def generate_html_for_snpedia_charts(rsnumbers):
#     print 'generating html for snpedia charts ...'
#
#     for idx, rs in enumerate(rsnumbers):
#         print 'processing snp {0} out of {1} ...'.format(idx + 1, len(rsnumbers))
#
#         page = open('snp_db/'+rs+'/snpedia_page.html').read()
#
#         soup = BeautifulSoup(page)
#
#         url_regex = r"'(http://chart\.apis\.google\.com/chart.*?)'"
#         m = re.search(url_regex, page)
#
#         if m:
#             tables = soup.find_all('table')
#             table = [table for table in tables if table.img and table.img.get('src') and re.match(r'http://chart\.apis\.google\.com/chart', table.img.get('src'))][0]
#             table.img['src'] = 'snpedia_chart.png'
#             open('snp_db/'+rs+'/snpedia_chart.html', 'w').write(table.prettify(formatter='html').encode('utf-8'))

def download_hapmap_data():
    if not os.path.exists('hapmap_archive'):
        print 'downloading and unpacking hapmap data ...'
        # get index
        query = 'http://hapmap.ncbi.nlm.nih.gov/downloads/frequencies/latest_phaseIII_ncbi_b36/fwd_strand/non-redundant/'
        response = urllib.urlopen(query)
        html = response.read()
        soup = BeautifulSoup(html)

        # create local archive
        prefix = 'http://hapmap.ncbi.nlm.nih.gov/downloads/frequencies/latest_phaseIII_ncbi_b36/fwd_strand/non-redundant/'
        os.mkdir('hapmap_archive')
        filenames = set(a['href'] for a in soup.find_all('a') if re.search(r'\.txt\.gz', a['href']))
        for idx, filename in enumerate(filenames): 
            print 'processing file {0} out of {1} ...'.format(idx + 1, len(filenames))
            urllib.urlretrieve(prefix + filename, 'hapmap_archive/' + filename)
            gunzip('hapmap_archive/' + filename)

def create_intermediate_hapmap_database():
    print 'creating intermediate hapmap database ...'
    conn = sqlite3.connect('hapmap.db') 
    c = conn.cursor()

    c.execute('''CREATE TABLE genotype
            (rs text, pop text, ref_allele_homo text, ref_allele_homo_freq real, ref_allele_hetero text, ref_allele_hetero_freq real, other_allele_homo text, other_allele_homo_freq real)''')

    c.execute('''CREATE TABLE allele
            (rs text, pop text, ref_allele text, ref_allele_freq real, other_allele text, other_allele_freq real)''')

    hapmap_files = os.listdir('hapmap_archive')
    for idx, f in enumerate(hapmap_files):
        print 'processing file {0} out of {1} ...'.format(idx + 1, len(hapmap_files))

        reader = csv.reader(open('hapmap_archive/' + f), delimiter=' ', quotechar='#')

        next(reader, None) # skip header
        for row in reader:

            pop = re.search(r'(ASW)|(CEU)|(CHB)|(CHD)|(GIH)|(JPT)|(LWK)|(MEX)|(MKK)|(TSI)|(YRI)', f).group(0)

            if re.search(r'genotype', f):
                c.execute('INSERT INTO genotype VALUES (?, ?, ?, ?, ?, ?, ?, ?)', (row[0], pop, row[10], row[11], row[13], row[14], row[16], row[17]))
            else:
                c.execute('INSERT INTO allele VALUES (?, ?, ?, ?, ?, ?)', (row[0], pop, row[10], row[11], row[13], row[14]))
    conn.commit()
    conn.close()

def add_final_hapmap_data(database, max_entries):
    print 'generating hapmap charts ...'

    conn = sqlite3.connect(database) 
    hapmap_conn = sqlite3.connect('hapmap.db') 
    c = conn.cursor()
    hapmap_c = hapmap_conn.cursor()

    c.execute('CREATE TABLE hapmap (rs text, html text, chart blob)')

    url_template = "http://chart.apis.google.com/chart?cht=bhs&chd=t:{0},{1},{2},{3},{4},{5},{6},{7},{8},{9},{10}|{11},{12},{13},{14},{15},{16},{17},{18},{19},{20},{21}|{22},{23},{24},{25},{26},{27},{28},{29},{30},{31},{32}&chs=275x200&chbh=8,5&chxl=0:|1:|{33}||&chxt=x,y&chco=CD853F,30FF30,0000FF,FF00FF&chls=1,1,0|1,1,0|1,1,0|1,1,0"
    html_template = '<table><tbody><tr><th class="text-center"><span style="font-size:1.25em"><span style="color:#CD853F">({0})</span><span style="color:#20D020">({1})</span><span style="color:#0000FF">({2})</span></span> </th></tr><tr><td colspan="3"><img src="{{{{hapmapImageSource}}}}"></td></tr></tbody></table>'
    populations = ['ASW','CEU','CHB','CHD','GIH','JPT','LWK','MEX','MKK','TSI','YRI']

    snps = list(hapmap_c.execute('SELECT DISTINCT rs FROM genotype'))

    if max_entries > 0:
        snps = snps[:max_entries]

    for idx, row in enumerate(snps): 
        if (idx + 1) % 100 == 0:
            print '{0} out of {1} snps processed ...'.format(idx + 1, len(snps))

        rs = row[0] 

        result = list(hapmap_c.execute('SELECT * FROM genotype WHERE rs = ?', (rs,)))

        # fill in missing data
        data = [] 
        for pop in populations:
            if pop in [row[1] for row in result]: 
                data.append([row for row in result if row[1] == pop][0])
            else:
                data.append([rs, pop, "", 0, "", 0, "", 0])

        # prepare url 
        format_parameters = []
        for i in [3, 5, 7]:
            for row in data:
                format_parameters.append(row[i]*100)
        format_parameters.append('|'.join(reversed(populations)))

        url = url_template.format(*format_parameters)

        # prepare html
        row = result[0]

        html = html_template.format(row[2], row[4], row[6])

        # prepare image
        image = urllib.urlopen(url).read()

        # insert both into the database 
        c.execute('INSERT INTO hapmap VALUES (?, ?, ?)', (rs, html, image))

    conn.commit()
    conn.close()
    hapmap_conn.close()

def download_and_add_hapmap_data(database, max_entries):
    download_hapmap_data()
    add_intermediate_hapmap_data(database)
    add_final_hapmap_data(database, max_entries)

def get_omim_text(rsnumbers):
    for rs in rsnumbers:
        query = 'http://www.ncbi.nlm.nih.gov/omim/?term='+rs+'&report=uilist&format=text'
        response = urllib2.urlopen(query)
        html = response.read()
        soup = BeautifulSoup(html)
        entry = soup.pre.text.split('\n')[0]
        if entry:
            url = 'http://api.omim.org/api/entry?apiKey=45CB5D7EF90D6522646B46F5095277D7B225F453&include=text&format=html&mimNumber=' + entry
            print url
            # urllib.urlretrieve(url, 'snp_db/' + rs + '/omim_entry.html')
            urllib.urlretrieve(url, 'test.html')

def download_and_add_dbsnp_data(database, max_entries):
    print 'creating dbsnp sqlite database ...'
    conn = sqlite3.connect(database) 
    c = conn.cursor()

    c.execute('''CREATE TABLE dbsnp
            (rs text, pop text, ref_allele text, ref_allele_freq real, other_allele text, other_allele_freq real)''')

    hapmap_files = os.listdir('archive')
    for idx, f in enumerate(hapmap_files):
        print 'processing file {0} out of {1} ...'.format(idx + 1, len(hapmap_files))

        reader = csv.reader(open('archive/' + f), delimiter=' ', quotechar='#')

        next(reader, None) # skip header
        for row in reader:

            pop = re.search(r'(ASW)|(CEU)|(CHB)|(CHD)|(GIH)|(JPT)|(LWK)|(MEX)|(MKK)|(TSI)|(YRI)', f).group(0)

            if re.search(r'genotype', f):
                c.execute('INSERT INTO genotype VALUES (?, ?, ?, ?, ?, ?, ?, ?)', (row[0], pop, row[10], row[11], row[13], row[14], row[16], row[17]))
            else:
                c.execute('INSERT INTO allele VALUES (?, ?, ?, ?, ?, ?)', (row[0], pop, row[10], row[11], row[13], row[14]))
    conn.commit()
    conn.close()

def generate_complete_database(database='snp_snip.db', max_entries=0):
    
    print 'generating database ' + database + ' in ' + os.getcwd() + ' ...'

    # download, process and add the data from snpedia 
    download_and_add_snpedia_data(database, max_entries)

    # download, process and add the data from hapmap
    download_and_add_hapmap_data(database, max_entries)

    # download, process and add the data from dbsnp
    # download_and_add_dbsnp_data(database, max_entries)

if __name__ == "__main__":
    if len(sys.argv) > 1:
        generate_complete_database(max_entries=int(sys.argv[1]))
    else:
        generate_complete_database()

