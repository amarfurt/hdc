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

// response stubs
// ok
var ok = function(response, data) {
	response.writeHead(200, {"Access-Control-Allow-Origin": "https://localhost:9000"});
	response.write(data);
	response.end();
}

// bad request
var badRequest = function(response, errorMessage) {
	console.error("[400 - bad request]: " + errorMessage);
	response.writeHead(400, {"Access-Control-Allow-Origin": "https://localhost:9000"});
	response.write(errorMessage);
	response.end();
}

// internal server error
var internalServerError = function(response, errorMessage) {
	console.error("[500 - internal server error]: " + errorMessage);
	response.writeHead(500, {"Access-Control-Allow-Origin": "https://localhost:9000"});
	response.write(errorMessage);
	response.end();
}

// parse request data and return corresponding json
var parseRequestBody = function(request, callback) {
	var payload = "";
	request.on("data", function(chunk) { payload += chunk; });
	request.on("error", function(error) { return {"error": error}; });
	request.on("end", function(chunk) {
		if (chunk) {
			payload += chunk; 
		}
		callback(JSON.parse(payload));
	});
}

// get tokens from database
var getTokens = function(response, userId, appId) {
	var request = http.request("http://localhost:9001/" + userId + "/apps/" + appId + "/tokens", function(getResponse) {
		parseRequestBody(getResponse, function(json) {
			if (json.error) {
				badRequest(response, "Failed to get tokens from database: " + json.error);
			} else {
				if (json.accessToken) {
					ok(response, JSON.stringify({"authorized": true}));
				} else {
					ok(response, JSON.stringify({"authorized": false}));
				}
			}
		});
	});
	request.end();
	request.on("error", function(err) { internalServerError(response, err); });
}

// get app details from database
var getAppDetails = function(response, userId, appId, code) {
	var data = JSON.stringify({
		"properties": {"_id": {"$oid": appId}},
		"fields": ["name", "type", "accessTokenUrl", "consumerKey", "consumerSecret"]
	});
	var postOptions = {
			"hostname": "localhost",
			"port": "9001",
			"path": "/apps/" + appId + "/details",
			"method": "POST",
			"headers": {
				"Content-Type": "application/json",
				"Content-Length": data.length
			}
	};
	var request = http.request(postOptions, function(postResponse) {
		parseRequestBody(postResponse, function(json) {
			if (json[0] && json[0].accessTokenUrl && json[0].consumerKey && json[0].consumerSecret) {
				requestAccessToken(response, userId, appId, code, json[0]);
			} else if (json.error) {
				badRequest(response, "Failed to get app details: " + json.error);
			} else {
				badRequest(response, "Failed to get app details.");
			}
		});
	});
	request.write(data);
	request.end();
	request.on("error", function(err) { internalServerError(response, err); });
}

// request permanent access token and save to database
var requestAccessToken = function(response, userId, appId, code, details) {
	
	// request access token
	var params = {
			"client_id": details.consumerKey, 
			"client_secret": details.consumerSecret,
			"grant_type": "authorization_code",
			"code": code
			};
	var requestUrl = details.accessTokenUrl + "?" + querystring.stringify(params);
	var request = https.request(requestUrl, function(tokenResponse) {
		parseRequestBody(tokenResponse, function(json) {
			if (json.access_token && json.refresh_token) {
				saveTokens(response, userId, appId, json);
			} else if (json.error_description) {
				badRequest(response, "Error received with response: " + json.error_description);
			} else if (json.error) {
				badRequest(response, "Error received with request: " + json.error);
			} else {
				badRequest(response, "Unknown error or incomplete response. Received:\n" + JSON.stringify(json));
			}
		});
	});
	request.end();
}

//save tokens to database
var saveTokens = function(response, userId, appId, tokens) {
	// construct header and data
	var data = JSON.stringify({
		"accessToken": tokens.access_token,
		"refreshToken": tokens.refresh_token
	});
	var postOptions = {
			"hostname": "localhost",
			"port": "9001",
			"path": "/" + userId + "/apps/" + appId + "/tokens",
			"method": "POST",
			"headers": {
				"Content-Type": "application/json",
				"Content-Length": data.length
			}
	};
	var request = http.request(postOptions, function(postResponse) {
		parseRequestBody(postResponse, function(json) {
			if (json.error) {
				badRequest(response, "Failed to save tokens to database: " + json.error);
			} else {
				ok(response, "");
			}
		});
	});
	request.write(data);
	request.end();
	request.on("error", function(err) { internalServerError(response, err); });
}

// create the server
https.createServer(sslOptions, function(request, response) {
	var requestUrl = url.parse(request.url);
	if (request.method === "OPTIONS") {
		// allow cross-origin requests
		response.writeHead(200, {
			"Access-Control-Allow-Origin": "https://localhost:9000",
			"Access-Control-Allow-Methods": "GET, POST",
			"Access-Control-Allow-Headers": "Content-Type"
		});
		response.end();
	} else if (request.method === "GET" && requestUrl.pathname.indexOf("authorized") > -1) {
		var split = requestUrl.pathname.split("/");
		getTokens(response, split[1], split[2]);
	} else if (request.method === "POST" && requestUrl.pathname.indexOf("accessToken") > -1) {
		// request the permanent access token
		var split = requestUrl.pathname.split("/");
		parseRequestBody(request, function(json) {
			if (json.code) {
				getAppDetails(response, split[1], split[2], json.code);
			} else {
				badRequest(response, "Code could not be parsed from request body.");
			}
		});
	} else {
		// ignore
	}
}).listen(5000);