HDC Platform
============

Starting the application
------------------------

First, start MongoDB (assuming the configuration file is stored in the data folder) ...

    mongod --config data/mongod.conf

..., the lighttpd web server (for apps and visualizations; config file in folder lighttpd) ...

    lighttpd -f lighttpd/lighttpd.conf

... and ElasticSearch. Use the -f option for interactive mode.

    elasticsearch [-f]

Now you can start the application from the project's root directory.

    play run


Load a sample database
----------------------

From the project directory, issue the command:

    mongorestore --drop --db healthdata dump/healthdata

This drops your current healthdata database and loads a sample version.
