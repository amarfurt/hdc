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