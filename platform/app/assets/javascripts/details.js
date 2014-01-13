var details = angular.module('details', []);
details.controller('RecordCtrl', ['$scope', '$http', '$sce', function($scope, $http, $sce) {
	// init
	$scope.error = null;
	$scope.record = {};
	
	// parse record id (format: /record/:id) and load the record
	var recordId = window.location.pathname.split("/")[2];
	var data = {"records": [recordId]};
	$http.post(jsRoutes.controllers.Records.get().url, data).
		success(function(data) {
			$scope.record = _.first(data);
			loadDetailsUrl();
		}).
		error(function(err) { $scope.error = "Failed to load record details: " + err; });
	
	loadDetailsUrl = function() {
		$http(jsRoutes.controllers.Records.getDetailsUrl($scope.record._id)).
			success(function(data) { $scope.record.url = $sce.trustAsResourceUrl(data);; }).
			error(function(err) { $scope.error = "Cannot display record details: " + err; });
	}
	
}]);
details.controller('UserCtrl', ['$scope', '$http', function($scope, $http) {
	// init
	$scope.error = null;
	$scope.user = {};
	
	// parse user id (format: /users/:id) and load the user details
	var userId = window.location.pathname.split("/")[2];
	var data = {"users": [userId]};
	$http.post(jsRoutes.controllers.Users.get().url, data).
		success(function(data) {
			$scope.error = null;
			$scope.user = _.first(data);
		}).
		error(function(err) { $scope.error = "Failed to load user details: " + err; });
}]);
details.controller('MessageCtrl', ['$scope', '$http', function($scope, $http) {
	// init
	$scope.error = null;
	$scope.message = {};
	
	// parse message id (format: /messages/:id) and load the app
	var messageId = window.location.pathname.split("/")[2];
	var data = {"messages": [messageId]};
	$http.post(jsRoutes.controllers.Messages.get().url, data).
		success(function(data) { $scope.message = _.first(data); }).
		error(function(err) { $scope.error = "Failed to load message details: " + err; });
}]);
details.controller('AppCtrl', ['$scope', '$http', function($scope, $http) {
	// init
	$scope.error = null;
	$scope.success = false;
	$scope.app = {};
	
	// parse app id (format: /apps/:id) and load the app
	var appId = window.location.pathname.split("/")[2];
	var data = {"apps": [appId]};
	$http.post(jsRoutes.controllers.Apps.get().url, data).
		success(function(data) {
			$scope.error = null;
			$scope.app = _.first(data);
			isInstalled();
		}).
		error(function(err) { $scope.error = "Failed to load app details: " + err; });
	
	isInstalled = function() {
		$http(jsRoutes.controllers.Apps.isInstalled($scope.app._id)).
			success(function(data) { $scope.app.installed = data; }).
			error(function(err) { $scope.error = "Failed to check whether this app is installed. " + err; });
	}
	
	$scope.install = function() {
		$http(jsRoutes.controllers.Apps.install($scope.app._id)).
			success(function() {
				$scope.app.installed = true;
				$scope.success = true;
			}).
			error(function(err) { $scope.error = "Failed to install the app: " + err; });
	}
	
	$scope.uninstall = function() {
		$http(jsRoutes.controllers.Apps.uninstall($scope.app._id)).
		success(function() {
			$scope.app.installed = false;
			$scope.success = false;
		}).
		error(function(err) { $scope.error = "Failed to uninstall the app: " + err; });
	}
	
}]);
details.controller('VisualizationCtrl', ['$scope', '$http', function($scope, $http) {
	// init
	$scope.error = null;
	$scope.success = false;
	$scope.visualization = {};
	
	// parse visualization id (format: /visualizations/:id) and load the visualization
	var visualizationId = window.location.pathname.split("/")[2];
	var data = {"visualizations": [visualizationId]};
	$http.post(jsRoutes.controllers.Visualizations.get().url, data).
		success(function(data) {
			$scope.error = null;
			$scope.visualization = _.first(data);
			isInstalled();
		}).
		error(function(err) { $scope.error = "Failed to load visualization details: " + err; });
	
	isInstalled = function() {
		$http(jsRoutes.controllers.Visualizations.isInstalled($scope.visualization._id)).
			success(function(data) { $scope.visualization.installed = data; }).
			error(function(err) { $scope.error = "Failed to check whether this visualization is installed. " + err; });
	}
	
	$scope.install = function() {
		$http(jsRoutes.controllers.Visualizations.install($scope.visualization._id)).
			success(function() {
				$scope.visualization.installed = true;
				$scope.success = true;
			}).
			error(function(err) { $scope.error = "Failed to install the visualization: " + err; });
	}
	
	$scope.uninstall = function() {
		$http(jsRoutes.controllers.Visualizations.uninstall($scope.visualization._id)).
		success(function() {
			$scope.visualization.installed = false;
			$scope.success = false;
		}).
		error(function(err) { $scope.error = "Failed to uninstall the visualization: " + err; });
	}
	
}]);
