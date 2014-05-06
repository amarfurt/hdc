from bs4 import BeautifulSoup
import urllib
import os
import csv
import json
import re
import sqlite3
from plumbum.cmd import gunzip

def download_hapmap_data():
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


def create_sqlite_database():
    print 'creating sqlite database ...'
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
    html_template = '<table><tbody><tr><th><span style="font-size:1.25em"><span style="color:#CD853F">({0})</span><span style="color:#20D020">({1})</span><span style="color:#0000FF">({2})</span></span> </th></tr><tr><td colspan="3"><img src="hapmap_chart.png"/></td></tr></tbody></table>'
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

generate_hapmap_charts()
