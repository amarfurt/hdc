/**
 * Utility methods for the node server.
 */

// ok
var ok = function(response, origin, data) {
	response.writeHead(200, {"Access-Control-Allow-Origin": origin});
	response.write(data);
	response.end();
}

// bad request
var badRequest = function(response, origin, errorMessage) {
	console.error("[400 - bad request]: " + errorMessage);
	response.writeHead(400, {"Access-Control-Allow-Origin": origin});
	response.write(errorMessage);
	response.end();
}

// internal server error
var internalServerError = function(response, origin, errorMessage) {
	console.error("[500 - internal server error]: " + errorMessage);
	response.writeHead(500, {"Access-Control-Allow-Origin": origin});
	response.write(errorMessage);
	response.end();
}

// parse request data and return corresponding json
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

// export all functions
exports.ok = ok;
exports.badRequest = badRequest;
exports.internalServerError = internalServerError;
exports.parseRequestBody = parseRequestBody;