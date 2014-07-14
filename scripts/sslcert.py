'''
Configuration of Lighttpd.

@author amarfurt
'''

import os
from command import Command

class SSLCertificate:

	def __init__(self, parentDir):
		self.parent = parentDir
		self.base = os.path.join(self.parent, 'ssl-certificate')

	def create(self):
		''' According to the steps at http://www.akadia.com/services/ssh_test_certificate.html '''
		print 'Creating self-signed SSL certificate...'
		if not os.path.exists(self.base):
			os.mkdir(self.base)
		print 'Generating a private key...'
		Command.execute('openssl genrsa -des3 -out server.key 1024', self.base)
		print 'Generating a certificate signing request (CSR)...'
		Command.execute('openssl req -new -key server.key -out server.csr', self.base)
		print 'Removing the passphrase from the key...'
		Command.execute('cp server.key server.key.org', self.base)
		Command.execute('openssl rsa -in server.key.org -out server.key', self.base)
		print 'Generating self-signed certificate...'
		Command.execute('openssl x509 -req -days 365 -in server.csr -signkey server.key -out server.crt', self.base)
		print 'Creating PEM file...'
		Command.execute('cat server.crt server.key > server.pem', self.base)
		print 'Generating the Java Key Store and importing the signed primary certificate...'
		Command.execute('keytool -import -trustcacerts -alias hdc -file server.crt -keystore server.keystore', self.base)
	