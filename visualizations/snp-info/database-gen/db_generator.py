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

def get_snp_names():
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

def create_local_database(rsnumbers):
    print 'creating local database ...'
    for rs in rsnumbers: 
        os.makedirs('snp_db/'+rs)

def download_snpedia_pages(rsnumbers):
    'downloading snpedia pages ...'

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

        open('snp_db/'+rs+'/snpedia_page.html', 'w').write(html)

def rs_filter(snp_names):
    return [snp for snp in snp_names if re.match(r'rs\d+', snp)]

def extract_text(html_page):

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

def generate_text_files(rsnumbers):
    print 'generating text files ...'

    for idx, rs in enumerate(rsnumbers):
        print 'processing snp {0} out of {1} ...'.format(idx + 1, len(rsnumbers))
        
        page = open('snp_db/'+rs+'/snpedia_page.html').read()
        open('snp_db/'+rs+'/snpedia_text.html', 'w').write(extract_text(page))

def generate_snpedia_charts(rsnumbers):
    print 'generating snpedia charts ...'

    for idx, rs in enumerate(rsnumbers):
        print 'processing snp {0} out of {1} ...'.format(idx + 1, len(rsnumbers))

        page = open('snp_db/'+rs+'/snpedia_page.html').read()


        m = re.search(url_regex, page)

        if m:
            soup = BeautifulSoup(m.group(1))
            urllib.urlretrieve(soup.string, 'snp_db/'+rs+'/snpedia_chart.png')

def generate_html_for_snpedia_charts(rsnumbers):
    print 'generating html for snpedia charts ...'

    for idx, rs in enumerate(rsnumbers):
        print 'processing snp {0} out of {1} ...'.format(idx + 1, len(rsnumbers))

        page = open('snp_db/'+rs+'/snpedia_page.html').read()

        soup = BeautifulSoup(page)

        url_regex = r"'(http://chart\.apis\.google\.com/chart.*?)'"
        m = re.search(url_regex, page)

        if m:
            tables = soup.find_all('table')
            table = [table for table in tables if table.img and table.img.get('src') and re.match(r'http://chart\.apis\.google\.com/chart', table.img.get('src'))][0]
            table.img['src'] = 'snpedia_chart.png'
            open('snp_db/'+rs+'/snpedia_chart.html', 'w').write(table.prettify(formatter='html').encode('utf-8'))

def download_hapmap_data():
    if not os.path.exists('archive'):
        print 'downloading and unpacking hapmap data ...'
        # get index
        query = 'http://hapmap.ncbi.nlm.nih.gov/downloads/frequencies/latest_phaseIII_ncbi_b36/fwd_strand/non-redundant/'
        response = urllib.urlopen(query)
        html = response.read()
        soup = BeautifulSoup(html)

        # create local archive
        prefix = 'http://hapmap.ncbi.nlm.nih.gov/downloads/frequencies/latest_phaseIII_ncbi_b36/fwd_strand/non-redundant/'
        os.mkdir('archive')
        filenames = set(a['href'] for a in soup.find_all('a') if re.search(r'\.txt\.gz', a['href']))
        for idx, filename in enumerate(filenames): 
            print 'processing file {0} out of {1} ...'.format(idx + 1, len(filenames))
            urllib.urlretrieve(prefix + filename, 'archive/' + filename)
            gunzip('archive/' + filename)


def create_hapmap_sqlite_database():
    if not os.path.isfile('hapmap.db'):
        print 'creating hapmap sqlite database ...'
        conn = sqlite3.connect('hapmap.db') 
        c = conn.cursor()

        c.execute('''CREATE TABLE genotype
                (rs text, pop text, ref_allele_homo text, ref_allele_homo_freq real, ref_allele_hetero text, ref_allele_hetero_freq real, other_allele_homo text, other_allele_homo_freq real)''')

        c.execute('''CREATE TABLE allele
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

def generate_hapmap_charts():
    print 'generating hapmap charts ...'
    conn = sqlite3.connect('hapmap.db') 
    c = conn.cursor()

    url_template = "http://chart.apis.google.com/chart?cht=bhs&chd=t:{0},{1},{2},{3},{4},{5},{6},{7},{8},{9},{10}|{11},{12},{13},{14},{15},{16},{17},{18},{19},{20},{21}|{22},{23},{24},{25},{26},{27},{28},{29},{30},{31},{32}&chs=275x200&chbh=8,5&chxl=0:|1:|{33}||&chxt=x,y&chco=CD853F,30FF30,0000FF,FF00FF&chls=1,1,0|1,1,0|1,1,0|1,1,0"
    html_template = '<table><tbody><tr><th class="text-center"><span style="font-size:1.25em"><span style="color:#CD853F">({0})</span><span style="color:#20D020">({1})</span><span style="color:#0000FF">({2})</span></span> </th></tr><tr><td colspan="3"><img src="{{{{hapmapImageSource}}}}"></td></tr></tbody></table>'
    populations = ['ASW','CEU','CHB','CHD','GIH','JPT','LWK','MEX','MKK','TSI','YRI']

    snps = list(c.execute('SELECT DISTINCT rs FROM genotype'))
    snpcount = len(snps)
    for idx, row in enumerate(snps): 
        if (idx + 1) % 100 == 0:
            print '{0} out of {1} snps processed ...'.format(idx + 1, snpcount)

        rs = row[0] 

        if os.path.isdir('snp_db/' + rs):
            result = list(c.execute('SELECT * FROM genotype WHERE rs = ?', (rs,)))

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

            # create the files

            urllib.urlretrieve(url, 'snp_db/' + rs + '/hapmap_chart.png')
            open('snp_db/' + rs + '/hapmap_chart.html', 'w').write(html)

    conn.close()

   
def generate_complete_database(max=0):
    
    print 'generating database in ' + os.getcwd() + ' ...'

    # get the names of all snps in snpedia
    snpedia = get_snp_names()
    # filter out snp names that aren't rs numbers
    rsnumbers = rs_filter(snpedia)
    
    # limit size
    if max > 0:
        rsnumbers = rsnumbers[:max]

    # initialize file structure
    create_local_database(rsnumbers)
    # load in the data from snpedia 
    download_snpedia_pages(rsnumbers)
    # extract text as html
    generate_text_files(rsnumbers)

    #hapmap charts
    download_hapmap_data()
    create_hapmap_sqlite_database()
    generate_hapmap_charts()

def get_omim_text(rsnumbers):
    for rs in rsnumbers:
        query = 'http://www.ncbi.nlm.nih.gov/omim/?term='+rs+'&report=uilist&format=text'
        response = urllib2.urlopen(query)
        html = response.read()
        soup = BeautifulSoup(html)
        entry = soup.pre.text.split('\n')[0]
        if entry:
            url = 'http://api.omim.org/api/entry?mimNumber=' + entry
            urllib.urlretrieve(url, 'snp_db/' + rs + '/omim_entry.html')

def create_dbsnp_sqlite_database():
    if not os.path.isfile('dbsnp.db'):
        print 'creating dbsnp sqlite database ...'
        conn = sqlite3.connect('hapmap.db') 
        c = conn.cursor()

        c.execute('''CREATE TABLE dbsnp_data
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

if __name__ == "__main__":
    if len(sys.argv) > 1:
        generate_complete_database(max=int(sys.argv[1]))
    else:
        generate_complete_database()

