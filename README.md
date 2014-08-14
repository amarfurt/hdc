Health Data Cooperative (HDC)
=============================

Manage, understand, and leverage your health data.

[![Build Status](https://drone.io/github.com/amarfurt/hdc/status.png)](https://drone.io/github.com/amarfurt/hdc/latest)

Scripts
-------
The main.py python script covers the basic tasks: setup, start, stop and resetting the database to an example state. It's been tested under Ubuntu. Required software:
- JDK 1.7+ (required by ElasticSearch)
- Python 2.7
- curl
- openssl (built on or after Apr 7, 2014)
- keytool

The script is called with
```
python main.py COMMAND [PRODUCT]
```
where ```COMMAND``` is one of
- ```setup```: Downloads the products' binaries, unpacks them and writes the paths in the config files. The only exception is Lighttpd, which is installed via the package manager APT.
- ```start```: Starts the products. Note: ElasticSearch has an initialization phase before it's completely ready. Activator starts the web application and leaves the user in interactive mode, where log output can be observed and the server is terminated with CTRL+D.
- ```stop```: Stops all products.
- ```reset```: Populates the database and the search server with sample data.
- ```dump```: Dumps the database and search server contents.

Giving a product is optional and will invoke the task only for the specified product, instead of for all. ```PRODUCT``` can be one of ```mongodb```, ```elasticsearch```, ```lighttpd```, ```node```, or ```activator```.

Folder structure
----------------

- apps: HDC apps (used to create/import health records).
- config: Template config files for products used by the platform.
- dump: Example database and search server state.
- logs (created by script): Logs of all products except web application (whose log is in platform -> logs).
- platform: Code for the HDC platform.
- scripts: Modules used by the main.py script.
- serverjs: Code for the Node.js server.
- ssl-certificate (created by script): Self-signed SSL certificate and Java Keystore.
- visualizations: HDC visualizations (used to display records).
