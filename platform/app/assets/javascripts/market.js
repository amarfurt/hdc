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
	$scope.registerApp = function() {
		if (!$scope.app.name || !$scope.app.description || !$scope.app.create || !$scope.app.details) {
			$scope.error = "Please fill in all required fields";
			return;
		}
		
		// send the request
		var data = {"name": $scope.app.name, "description": $scope.app.description, "create": $scope.app.create, "details": $scope.app.details};
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
		if (!$scope.visualization.name || !$scope.visualization.description || !$scope.visualization.url) {
			$scope.error = "Please fill in all required fields";
			return;
		}
		
		// send the request
		var data = {"name": $scope.visualization.name, "description": $scope.visualization.description, "url": $scope.visualization.url};
		$http.post(jsRoutes.controllers.Market.registerVisualization().url, data).
			success(function(redirectUrl) { window.location.replace(redirectUrl); }).
			error(function(err) { $scope.error = "Failed to register visualization: " + err; });
	}
	
}]);