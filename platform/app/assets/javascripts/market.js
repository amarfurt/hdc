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
	
	// fetch apps and visualizations
	$http(jsRoutes.controllers.Apps.getSpotlighted()).
		success(function(data) { $scope.apps.spotlighted = data; }).
		error(function(err) { $scope.error = "Failed to load apps: " + err; });
	$http(jsRoutes.controllers.Visualizations.getSpotlighted()).
		success(function(data) { $scope.visualizations.spotlighted = data; }).
		error(function(err) { $scope.error = "Failed to load visualizations: " + err; });
	
	// show app details
	$scope.showAppDetails = function(app) {
		window.location.href = jsRoutes.controllers.Apps.details(app._id).url;
	}
	
	// show visualization details
	$scope.showVisualizationDetails = function(visualization) {
		window.location.href = jsRoutes.controllers.Visualizations.details(visualization._id).url;
	}
	
}]);
market.controller('RegisterAppCtrl', ['$scope', '$http', function($scope, $http) {
	
	// init
	$scope.error = null;
	$scope.app = {};
	
	// register app
	$scope.registerApp = function() {
		if (!app.name || !app.description || !app.create || !app.details) {
			$scope.error = "Please fill in all required fields";
			return;
		}
		
		// send the request
		var data = {"name": app.name, "description": app.description, "create": app.create, "details": app.details};
		$http.post(jsRoutes.controllers.Market.registerApp().url, data).
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
		if (!visualization.name || !visualization.description || !visualization.url) {
			$scope.error = "Please fill in all required fields";
			return;
		}
		
		// send the request
		var data = {"name": visualization.name, "description": visualization.description, "url": visualization.url};
		$http.post(jsRoutes.controllers.Market.registerVisualization().url, data).
			success(function(redirectUrl) { window.location.replace(redirectUrl); }).
			error(function(err) { $scope.error = "Failed to register visualization: " + err; });
	}
	
}]);