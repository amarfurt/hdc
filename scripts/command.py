'''
Utility class for executing SSH/shell commands.

@author amarfurt
'''

import sys, subprocess

class Command:

	@staticmethod
	def execute(command, workingDirectory=None, host=None, verbose=False):
		""" Execute given command in the shell """
		if workingDirectory != None:
			command = "cd '" + workingDirectory + "'; " + command
		if host != None:
			# Needs password-less SSH
			command = "ssh " + host + " '" + command + "'"
		if verbose:
			print 'Executing: ' + command
		subprocess.call(command, shell=True, stdout=sys.stdout, stderr=sys.stderr)
