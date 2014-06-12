/**
 * Server specific settings.
 */

// required modules
var fs = require("fs");

// settings
var localhost = "localhost";
// var localhost = "129.132.227.148";
var nodePort = 5000;

// ssl certificate
var sslOptions = {
		key: fs.readFileSync("/home/amarfurt/ssl-certificate/server.key"),
		cert: fs.readFileSync("/home/amarfurt/ssl-certificate/server.crt")
}

// export settings
exports.localhost = localhost;
exports.sslOptions = sslOptions;
exports.nodePort = nodePort;