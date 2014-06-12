/**
 * Router that delegates the requests to the respective module.
 */

// required modules
var url = require("url");
var oauth2 = require("./oauth2");

// route to respective module
var process = function(request, response) {
	var requestUrl = url.parse(request.url);
	if (requestUrl.pathname.indexOf("oauth2") === 1) {
		oauth2.process(request, response);
	} else {
		// ignore other requests
	}
}
exports.process = process;