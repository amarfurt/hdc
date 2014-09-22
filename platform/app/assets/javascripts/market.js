var market = angular.module('market', []);
market.controller('MarketCtrl', ['$scope', '$http', function($scope, $http) {
	
	// init
	$scope.error = null;
	$scope.apps = {};
	$scope.apps.spotlighted = [];
	$scope.apps.suggested = [];
	$scope.visualizations = {};
	$scope.visualizations.spotlighted = [];
	$scope.visualizations.suggested = [];
	
	// get apps and visualizations
	var properties = {"spotlighted": true};
	var fields = ["name", "description"];
	var data = {"properties": properties, "fields": fields};
	$http.post(jsRoutes.controllers.Apps.get().url, JSON.stringify(data)).
		success(function(apps) { $scope.apps.spotlighted = apps; }).
		error(function(err) { $scope.error = "Failed to load apps: " + err; });
	$http.post(jsRoutes.controllers.Visualizations.get().url, JSON.stringify(data)).
		success(function(visualizations) { $scope.visualizations.spotlighted = visualizations; }).
		error(function(err) { $scope.error = "Failed to load visualizations: " + err; });
	
	// show app details
	$scope.showAppDetails = function(app) {
		window.location.href = jsRoutes.controllers.Apps.details(app._id.$oid).url;
	}
	
	// show visualization details
	$scope.showVisualizationDetails = function(visualization) {
		window.location.href = jsRoutes.controllers.Visualizations.details(visualization._id.$oid).url;
	}
	
}]);
market.controller('RegisterAppCtrl', ['$scope', '$http', function($scope, $http) {
	
	// init
	$scope.error = null;
	$scope.app = {};
	
	// register app
	$scope.registerApp = function(type) {
		// check required fields
		if (!$scope.app.filename || !$scope.app.name || !$scope.app.description) {
			$scope.error = "Please fill in all required fields";
			return;
		} else if (type === "create" && !$scope.app.createUrl) {
			$scope.error = "Please fill in all required fields";
			return;
		} else if (type === "oauth1" && (!$scope.app.authorizationUrl || !$scope.app.accessTokenUrl || !$scope.app.consumerKey)) {
			$scope.error = "Please fill in all required fields";
			return;
		} else if (type === "oauth2" && (!$scope.app.authorizationUrl || !$scope.app.accessTokenUrl || !$scope.app.consumerKey || !$scope.app.consumerSecret || !$scope.app.scopeParameters)) {
			$scope.error = "Please fill in all required fields";
			return;
		}
		
		// piece together data object
		if (type === "create") {
			var data = {
					"filename": $scope.app.filename,
					"name": $scope.app.name,
					"description": $scope.app.description,
					"createUrl": $scope.app.createUrl
			};
		} else if (type === "oauth1") {
			var data = {
					"filename": $scope.app.filename,
					"name": $scope.app.name,
					"description": $scope.app.description,
					"authorizationUrl": $scope.app.authorizationUrl,
					"accessTokenUrl": $scope.app.accessTokenUrl,
					"consumerKey": $scope.app.consumerKey
			};
		} else if (type === "oauth2") {
			var data = {
					"filename": $scope.app.filename,
					"name": $scope.app.name,
					"description": $scope.app.description,
					"authorizationUrl": $scope.app.authorizationUrl,
					"accessTokenUrl": $scope.app.accessTokenUrl,
					"consumerKey": $scope.app.consumerKey,
					"consumerSecret": $scope.app.consumerSecret,
					"scopeParameters": $scope.app.scopeParameters
			};
		}
		
		// send the request
		$http.post(jsRoutes.controllers.Market.registerApp(type).url, data).
			success(function(redirectUrl) { window.location.replace(redirectUrl); }).
			error(function(err) { $scope.error = "Failed to register app: " + err; });
	}
	
}]);
market.controller('RegisterVisualizationCtrl', ['$scope', '$http', function($scope, $http) {
	
	// init
	$scope.error = null;
	$scope.visualization = {};
	
	// register visualization
	$scope.registerVisualization = function() {
		if (!$scope.visualization.filename || !$scope.visualization.name || !$scope.visualization.description || !$scope.visualization.url) {
			$scope.error = "Please fill in all required fields";
			return;
		}
		
		// send the request
		var data = {
				"filename": $scope.visualization.filename,
				"name": $scope.visualization.name,
				"description": $scope.visualization.description,
				"url": $scope.visualization.url
		};
		$http.post(jsRoutes.controllers.Market.registerVisualization().url, data).
			success(function(redirectUrl) { window.location.replace(redirectUrl); }).
			error(function(err) { $scope.error = "Failed to register visualization: " + err; });
	}
	
}]);