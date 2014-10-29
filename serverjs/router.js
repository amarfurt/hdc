/**
 * Router that delegates the requests to the respective module.
 */

// required modules
var url = require("url");
var snp_snip = require("../visualizations/snp-snip/js/node.js");

// route to respective module
var process = function(request, response) {
	var requestUrl = url.parse(request.url);
	if (requestUrl.pathname.indexOf("snp-snip") === 1) {
		snp_snip.process(request, response);
	} else {
		// ignore other requests
	}
}
exports.process = process;