HDC Platform
============

Starting the application
------------------------

From the project root, start MongoDB (assuming the folder layout of the hdc repository) ...

    mongod --config ../config-files/mongod.conf

..., the lighttpd web server (for apps and visualizations) ...

    lighttpd -f ../config-files/lighttpd.conf

... and ElasticSearch. Use the -f option for interactive mode.

    elasticsearch [-f]

Now you can start the application from the project's root directory.

    play run


Load a sample database
----------------------

From the project directory, issue the command:

    mongorestore --drop --db healthdata dump/healthdata

This drops your current healthdata database and loads a sample version.
