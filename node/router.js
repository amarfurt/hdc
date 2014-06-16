/**
 * Router that delegates the requests to the respective module.
 */

// required modules
var url = require("url");
var oauth2 = require("./oauth2");
var record_list = require("../visualizations/record-list/js/node.js");
var run_aggregator = require("../visualizations/run-aggregator/js/node.js");

// route to respective module
var process = function(request, response) {
	var requestUrl = url.parse(request.url);
	if (requestUrl.pathname.indexOf("oauth2") === 1) {
		oauth2.process(request, response);
	} else if (requestUrl.pathname.indexOf("record-list") === 1) {
		record_list.process(request, response);
	} else if (requestUrl.pathname.indexOf("run-aggregator") === 1) {
		run_aggregator.process(request, response);
	} else {
		// ignore other requests
	}
}
exports.process = process;