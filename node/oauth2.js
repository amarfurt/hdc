// required modules
var https = require("https");
var http = require("http");
var fs = require("fs");
var url = require("url");
var querystring = require("querystring");

// ssl certificate
var sslOptions = {
		key: fs.readFileSync("/home/amarfurt/ssl-certificate/server.key"),
		cert: fs.readFileSync("/home/amarfurt/ssl-certificate/server.crt")
}

// HELPER METHODS
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

// TASKS
// get access token from database
var getAccessToken = function(response, origin, params) {
	var request = http.request("http://localhost:9001/" + params.userId + "/apps/" + params.appId + "/tokens", function(getResponse) {
		parseRequestBody(getResponse, function(json) {
			if (json.accessToken) {
				params.accessToken = json.accessToken;
				var next = params.queue.shift();
				next(response, origin, params);
			} else if (json.error) {
				badRequest(response, origin, "Failed to get tokens from database: " + json.error);
			} else {
				badRequest(response, origin, "No access token found.");
			}
		});
	});
	request.end();
	request.on("error", function(err) { internalServerError(response, origin, err); });
}

// imports the data 
var getData = function(response, origin, params) {
	var options = url.parse(params.endpoint);
	options.headers = {"Authorization": "Bearer " + params.accessToken};
	var request = https.request(options, function(getResponse) {
		parseRequestBody(getResponse, function(json) {
			if (json.error) {
				badRequest(response, origin, "Failed to get the data: " + json.error);
			} else {
				ok(response, origin, JSON.stringify(json));
			}
		});
	});
	request.end();
	request.on("error", function(err) { internalServerError(response, origin, err); });
}

// get app details from database
var getAppDetails = function(response, origin, params) {
	var data = JSON.stringify({
		"properties": {"_id": {"$oid": params.appId}},
		"fields": ["name", "type", "accessTokenUrl", "consumerKey", "consumerSecret"]
	});
	var options = url.parse("http://localhost:9001/apps/" + params.appId + "/details");
	options.method = "POST";
	options.headers = {
			"Content-Type": "application/json",
			"Content-Length": data.length
	};
	var request = http.request(options, function(postResponse) {
		parseRequestBody(postResponse, function(json) {
			if (json[0] && json[0].accessTokenUrl && json[0].consumerKey && json[0].consumerSecret) {
				params.accessTokenUrl = json[0].accessTokenUrl;
				params.consumerKey = json[0].consumerKey;
				params.consumerSecret = json[0].consumerSecret;
				var next = params.queue.shift();
				next(response, origin, params);
			} else if (json.error) {
				badRequest(response, origin, "Failed to get app details: " + json.error);
			} else {
				badRequest(response, origin, "Failed to get the complete app details.");
			}
		});
	});
	request.write(data);
	request.end();
	request.on("error", function(err) { internalServerError(response, origin, err); });
}

// request permanent access token and save to database
var requestAccessToken = function(response, origin, params) {
	var queryParams = {
			"client_id": params.consumerKey, 
			"client_secret": params.consumerSecret,
			"grant_type": "authorization_code",
			"code": params.code
			};
	var requestUrl = params.accessTokenUrl + "?" + querystring.stringify(queryParams);
	var request = https.request(requestUrl, function(tokenResponse) {
		parseRequestBody(tokenResponse, function(json) {
			if (json.access_token && json.refresh_token) {
				params.tokens = {"accessToken": json.access_token, "refreshToken": json.refresh_token};
				var next = params.queue.shift();
				next(response, origin, params);
			} else if (json.error_description) {
				badRequest(response, origin, "Error received with response: " + json.error_description);
			} else if (json.error) {
				badRequest(response, origin, "Error received with request: " + json.error);
			} else {
				badRequest(response, origin, "Unknown error or incomplete response. Received:\n" + JSON.stringify(json));
			}
		});
	});
	request.end();
	request.on("error", function(err) { internalServerError(response, origin, err); });
}

//save tokens to database
var saveTokens = function(response, origin, params) {
	// construct header and data
	var data = JSON.stringify(params.tokens);
	var options = url.parse("http://localhost:9001/" + params.userId + "/apps/" + params.appId + "/tokens");
	options.method = "POST";
	options.headers = {
			"Content-Type": "application/json",
			"Content-Length": data.length
	};
	var request = http.request(options, function(postResponse) {
		if (postResponse.statusCode === 200) {
			ok(response, origin, "");
		} else {
			badRequest(response, origin, "Failed to save tokens to database: " + json.error);
		}
	});
	request.write(data);
	request.end();
	request.on("error", function(err) { internalServerError(response, origin, err); });
}

// create the server
https.createServer(sslOptions, function(request, response) {
	var origin = request.headers.origin;
	var requestUrl = url.parse(request.url);
	if (request.method === "OPTIONS") {
		// allow cross-origin requests
		response.writeHead(200, {
			"Access-Control-Allow-Origin": origin,
			"Access-Control-Allow-Methods": "GET, POST",
			"Access-Control-Allow-Headers": "Content-Type"
		});
		response.end();
	} else if (request.method === "GET" && requestUrl.pathname.indexOf("data") > -1) {
		// get the data
		var split = requestUrl.pathname.split("/");
		var params = {
				"userId": split[2],
				"appId": split[3],
				"endpoint": decodeURIComponent(split[4]),
				"queue": [getData]
		};
		getAccessToken(response, origin, params);
	} else if (request.method === "POST" && requestUrl.pathname.indexOf("accessToken") > -1) {
		// request the permanent access token
		var split = requestUrl.pathname.split("/");
		var params = {
				"userId": split[2],
				"appId": split[3],
				"queue": [requestAccessToken, saveTokens]
		};
		parseRequestBody(request, function(json) {
			if (json.code) {
				params.code = json.code;
				getAppDetails(response, origin, params);
			} else {
				badRequest(response, origin, "Code could not be parsed from request body.");
			}
		});
	} else {
		// ignore other requests
	}
}).listen(5000);