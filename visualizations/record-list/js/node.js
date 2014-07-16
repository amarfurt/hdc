/**
 * Get records to be displayed with the record list visualization.
 * Skip records that do not have the correct format.
 */

// settings
var localhost = "localhost";
var port = "9001";
var path = "/cache/";

// required modules
var http = require("http");
var url = require("url");

// utils
//parse request data and return corresponding json
var parseRequestBody = function(request, callback) {
	var payload = "";
	request.on("data", function(chunk) { payload += chunk; });
	request.on("error", function(error) { callback({"error": error}); });
	request.on("end", function(chunk) {
		if (chunk) {
			payload += chunk; 
		}
		var json = null;
		if (!request.statusCode || request.statusCode === 200) {
			try {
				json = JSON.parse(payload);
			} catch (exception) {
				json = {"error": exception.message};
			}
		} else {
			json = {"error": payload};
		}
		callback(json);
	});
}

// ok
var ok = function(response, origin, data) {
	response.writeHead(200, {"Access-Control-Allow-Origin": origin});
	response.write(data);
	response.end();
}

// bad request
var badRequest = function(response, origin, errorMessage) {
	response.writeHead(400, {"Access-Control-Allow-Origin": origin});
	response.write(errorMessage);
	response.end();
}

//internal server error
var internalServerError = function(response, origin, errorMessage) {
	response.writeHead(500, {"Access-Control-Allow-Origin": origin});
	response.write(errorMessage);
	response.end();
}

// process request
var process = function(request, response) {
	var origin = request.headers.origin;
	var requestUrl = url.parse(request.url);
	var parts = requestUrl.pathname.split("/");
	if (request.method === "GET" && parts[1] === "record-list") {
		// get cached records with cache id
		var request = http.request("http://" + localhost + ":" + port + path + parts[2], function(getResponse) {
			parseRequestBody(getResponse, function(json) {
				if (!json.error) {
					// test record format; skip other formats
					var records = [];
					for (var i = 0; i < json.length; i++) {
						var data = json[i].data;
						if (data) {
							try {
								var record = JSON.parse(data);
							} catch (exception) {
								// skip
								continue;
							}
							if (record.title && record.data) {
								records.push(record);
							}
						}
					}
					ok(response, origin, JSON.stringify(records));
				} else {
					badRequest(response, origin, json.error);
				}
			});
		});
		request.end();
		request.on("error", function(err) { internalServerError(response, origin, err); });
	} else {
		// ignore other requests
	}
}
exports.process = process;
