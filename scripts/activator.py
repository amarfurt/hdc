'''
Configuration of Activator for the Play Framework.

@author amarfurt
'''

import os
from product import Product
from command import Command
from sslcert import SSLCertificate

class Activator(Product):

	def __init__(self, parentDir):
		self.parent = parentDir
		self.base = os.path.join(self.parent, 'activator')
		self.bin = os.path.join(self.base, 'activator')
		self.code = os.path.join(self.parent, 'platform')
		self.keystore = os.path.join(SSLCertificate(self.parent).base, 'server.keystore')

	def setup(self, version):
		print 'Setting up Activator...'
		print 'Downloading binaries...'
		Command.execute('wget http://downloads.typesafe.com/typesafe-activator/{0}/typesafe-activator-{0}-minimal.zip'
			.format(version), self.parent)
		print 'Extracting...'
		Command.execute('unzip typesafe-activator-{0}-minimal.zip'.format(version), self.parent)
		print 'Setting symlink...'
		Command.execute('ln -s activator-{0}-minimal activator'.format(version), self.parent)
		print 'Cleaning up...'
		Command.execute('rm typesafe-activator-{0}-minimal.zip'.format(version), self.parent)

	def start(self):
		print 'Starting Activator...'
		# Command.execute(self.bin + ' start', self.platform)
		Command.execute('{0} run -Dhttp.port=9001 -Dhttps.port=9000 -Dhttps.keyStore={1} -Dhttps.keyStorePassword=secret'
			.format(self.bin, self.keystore), self.code)

	def stop(self):
		print 'Shutting down Activator...'
		# somehow pgrep gets the correct process but pkill doesn't kill it...
		# using this workaround for now:
		Command.execute('pgrep -f activator | xargs kill -9')

	def reset(self):
		pass
