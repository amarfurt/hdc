/**
 * OAuth 2.0 module.
 */

// required modules
var https = require("https");
var http = require("http");
var url = require("url");
var querystring = require("querystring");
var settings = require("./settings");
var utils = require("./utils");

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
		utils.parseRequestBody(postResponse, function(json) {
			if (json[0] && json[0].accessTokenUrl && json[0].consumerKey && json[0].consumerSecret) {
				params.accessTokenUrl = json[0].accessTokenUrl;
				params.consumerKey = json[0].consumerKey;
				params.consumerSecret = json[0].consumerSecret;
				var next = params.queue.shift();
				next(response, origin, params);
			} else if (json.error) {
				utils.badRequest(response, origin, "Failed to get app details: " + json.error);
			} else {
				utils.badRequest(response, origin, "Failed to get the complete app details.");
			}
		});
	});
	request.write(data);
	request.end();
	request.on("error", function(err) { utils.internalServerError(response, origin, err); });
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
		utils.parseRequestBody(tokenResponse, function(json) {
			if (json.access_token && json.refresh_token) {
				params.tokens = {"accessToken": json.access_token, "refreshToken": json.refresh_token};
				var next = params.queue.shift();
				next(response, origin, params);
			} else if (json.error_description) {
				utils.badRequest(response, origin, "Error received with response: " + json.error_description);
			} else if (json.error) {
				utils.badRequest(response, origin, "Error received with request: " + json.error);
			} else {
				utils.badRequest(response, origin, "Unknown error or incomplete response. Received:\n" + JSON.stringify(json));
			}
		});
	});
	request.end();
	request.on("error", function(err) { utils.internalServerError(response, origin, err); });
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
			utils.ok(response, origin, "");
		} else {
			utils.badRequest(response, origin, "Failed to save tokens to database: " + json.error);
		}
	});
	request.write(data);
	request.end();
	request.on("error", function(err) { utils.internalServerError(response, origin, err); });
}

// process incoming requests
var process = function(request, response) {
	var origin = request.headers.origin;
	var requestUrl = url.parse(request.url);
	if (request.method === "POST" && requestUrl.pathname.indexOf("accessToken") > -1) {
		// request the permanent access token
		var split = requestUrl.pathname.split("/");
		var params = {
				"userId": split[3],
				"appId": split[4],
				"queue": [requestAccessToken, saveTokens]
		};
		utils.parseRequestBody(request, function(json) {
			if (json.code) {
				params.code = json.code;
				getAppDetails(response, origin, params);
			} else {
				utils.badRequest(response, origin, "Code could not be parsed from request body.");
			}
		});
	} else {
		// ignore other requests
	}
}
exports.process = process;
