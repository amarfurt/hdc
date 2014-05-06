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



if __name__ == "__main__":
    if len(sys.argv) > 1:
        generate_complete_database(max=int(sys.argv[1]))
    else:
        generate_complete_database()

