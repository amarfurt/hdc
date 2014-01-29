var details = angular.module('details', []);
details.controller('RecordCtrl', ['$scope', '$http', '$sce', function($scope, $http, $sce) {
	// init
	$scope.error = null;
	$scope.record = {};
	
	// parse record id (format: /record/:id) and load the record
	var recordId = window.location.pathname.split("/")[2];
	var properties = {"_id": {"$oid": recordId}};
	var fields = ["name", "owner", "app", "creator", "created", "data"];
	var data = {"properties": properties, "fields": fields};
	$http.post(jsRoutes.controllers.Records.get().url, JSON.stringify(data)).
		success(function(records) {
			$scope.record = records[0];
			loadUserNames();
			loadAppName();
			rewriteCreated();
			loadDetailsUrl();
		}).
		error(function(err) { $scope.error = "Failed to load record details: " + err; });
	
	loadUserNames = function() {
		var data = {"properties": {"_id": [$scope.record.owner, $scope.record.creator]}, "fields": ["name"]};
		$http.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data)).
			success(function(users) {
				_.each(users, function(user) {
					if ($scope.record.owner.$oid === user._id.$oid) { $scope.record.owner = user.name; }
					if ($scope.record.creator.$oid === user._id.$oid) { $scope.record.creator = user.name; }
				});
			}).
			error(function(err) { $scope.error = "Failed to load names: " + err; });
	}
	
	loadAppName = function() {
		var data = {"properties": {"_id": $scope.record.app}, "fields": ["name"]};
		$http.post(jsRoutes.controllers.Apps.get().url, JSON.stringify(data)).
			success(function(apps) { $scope.record.app = apps[0].name; }).
			error(function(err) { $scope.error = "Failed to load app name: " + err; });
	}
	
	loadDetailsUrl = function() {
		$http(jsRoutes.controllers.Records.getDetailsUrl($scope.record._id.$oid)).
			success(function(url) { $scope.record.url = $sce.trustAsResourceUrl(url); }).
			error(function(err) { $scope.error = "Failed to load record details: " + err; });
	}
	
	rewriteCreated = function() {
		var split = $scope.record.created.split(" ");
		$scope.record.created = split[0] + " at " + split[1];
	}
	
}]);
details.controller('UserCtrl', ['$scope', '$http', function($scope, $http) {
	// init
	$scope.error = null;
	$scope.user = {};
	
	// parse user id (format: /users/:id) and load the user details
	var userId = window.location.pathname.split("/")[2];
	var data = {"properties": {"_id": {"$oid": userId}}, "fields": ["name", "email"]};
	$http.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data)).
		success(function(users) { $scope.user = users[0]; }).
		error(function(err) { $scope.error = "Failed to load user details: " + err; });
	
}]);
details.controller('MessageCtrl', ['$scope', '$http', function($scope, $http) {
	// init
	$scope.error = null;
	$scope.message = {};
	
	// parse message id (format: /messages/:id) and load the app
	var messageId = window.location.pathname.split("/")[2];
	var data = {"properties": {"_id": {"$oid": messageId}}, "fields": ["sender", "receivers", "created", "title", "content"]};
	$http.post(jsRoutes.controllers.Messages.get().url, JSON.stringify(data)).
		success(function(messages) {
			$scope.message = messages[0];
			getUserNames();
			rewriteCreated();
		}).
		error(function(err) { $scope.error = "Failed to load message details: " + err; });
	
	getUserNames = function() {
		var data = {"properties": {"_id": _.flatten([$scope.message.sender, $scope.message.receivers])}, "fields": ["name"]};
		$http.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data)).
			success(function(users) {
				_.each(users, function(user) {
					if ($scope.message.sender.$oid === user._id.$oid) {
						$scope.message.sender.name = user.name;
					} else {
						var receiver = _.find($scope.message.receivers, function(rec) { return rec.$oid === user._id.$oid; });
						receiver.name = user.name;
					}
				});
			}).
			error(function(err) { $scope.error = "Failed to load sender and receiver names: " + err; });
	}
	
	rewriteCreated = function() {
		var split = $scope.message.created.split(" ");
		$scope.message.created = split[0] + " at " + split[1];
	}
	
}]);
details.controller('AppCtrl', ['$scope', '$http', function($scope, $http) {
	// init
	$scope.error = null;
	$scope.success = false;
	$scope.app = {};
	
	// parse app id (format: /apps/:id) and load the app
	var appId = window.location.pathname.split("/")[2];
	var data = {"properties": {"_id": {"$oid": appId}}, "fields": ["name", "creator", "description"]};
	$http.post(jsRoutes.controllers.Apps.get().url, JSON.stringify(data)).
		success(function(apps) {
			$scope.error = null;
			$scope.app = apps[0];
			isInstalled();
			getCreatorName();
		}).
		error(function(err) { $scope.error = "Failed to load app details: " + err; });
	
	isInstalled = function() {
		$http(jsRoutes.controllers.Apps.isInstalled($scope.app._id.$oid)).
			success(function(installed) { $scope.app.installed = installed; }).
			error(function(err) { $scope.error = "Failed to check whether this app is installed: " + err; });
	}
	
	getCreatorName = function() {
		var data = {"properties": {"_id": $scope.app.creator}, "fields": ["name"]};
		$http.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data)).
			success(function(users) { $scope.app.creator = users[0].name; }).
			error(function(err) { $scope.error = "Failed to load the name of the creator: " + err; })
	}
	
	$scope.install = function() {
		$http(jsRoutes.controllers.Apps.install($scope.app._id.$oid)).
			success(function() {
				$scope.app.installed = true;
				$scope.success = true;
			}).
			error(function(err) { $scope.error = "Failed to install the app: " + err; });
	}
	
	$scope.uninstall = function() {
		$http(jsRoutes.controllers.Apps.uninstall($scope.app._id.$oid)).
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
	var data = {"properties": {"_id": {"$oid": visualizationId}}, "fields": ["name", "creator", "description"]};
	$http.post(jsRoutes.controllers.Visualizations.get().url, JSON.stringify(data)).
		success(function(visualizations) {
			$scope.error = null;
			$scope.visualization = visualizations[0];
			isInstalled();
			getCreatorName();
		}).
		error(function(err) { $scope.error = "Failed to load visualization details: " + err; });
	
	isInstalled = function() {
		$http(jsRoutes.controllers.Visualizations.isInstalled($scope.visualization._id.$oid)).
			success(function(installed) { $scope.visualization.installed = installed; }).
			error(function(err) { $scope.error = "Failed to check whether this visualization is installed: " + err; });
	}
	
	getCreatorName = function() {
		var data = {"properties": {"_id": $scope.visualization.creator}, "fields": ["name"]};
		$http.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data)).
			success(function(users) { $scope.visualization.creator = users[0].name; }).
			error(function(err) { $scope.error = "Failed to load the name of the creator: " + err; })
	}
	
	$scope.install = function() {
		$http(jsRoutes.controllers.Visualizations.install($scope.visualization._id.$oid)).
			success(function() {
				$scope.visualization.installed = true;
				$scope.success = true;
			}).
			error(function(err) { $scope.error = "Failed to install the visualization: " + err; });
	}
	
	$scope.uninstall = function() {
		$http(jsRoutes.controllers.Visualizations.uninstall($scope.visualization._id.$oid)).
		success(function() {
			$scope.visualization.installed = false;
			$scope.success = false;
		}).
		error(function(err) { $scope.error = "Failed to uninstall the visualization: " + err; });
	}
	
}]);
