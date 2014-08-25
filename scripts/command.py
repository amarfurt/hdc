'''
Utility class for executing SSH/shell commands.

@author amarfurt
'''

import sys, subprocess

class Command:

	@staticmethod
	def execute(command, workingDirectory=None, host=None, redirect=None, verbose=False):
		""" Execute given command in the shell """
		if workingDirectory:
			command = "cd '" + workingDirectory + "'; " + command
		if host:
			# Needs password-less SSH
			command = "ssh " + host + " '" + command + "'"
		if redirect:
			redirect_file = open(redirect, 'w')
			std_out = redirect_file
			std_err = redirect_file
		else:
			std_out = sys.stdout
			std_err = sys.stderr
		if verbose:
			print 'Executing: ' + command
		subprocess.call(command, shell=True, stdout=std_out, stderr=std_err)
