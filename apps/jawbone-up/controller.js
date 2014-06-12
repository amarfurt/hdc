var jawboneUp = angular.module('jawbone-up', ['ngRoute']);
jawboneUp.config(['$routeProvider', function($routeProvider) {
	$routeProvider.
		when('/import/:userId/:appId/:replyTo', {
			templateUrl: 'views/import.html',
			controller: 'ImportCtrl'
		}).
		when('/details/:record', {
			templateUrl: 'views/details.html',
			controller: 'DetailsCtrl'
		});
}]);

jawboneUp.controller('ImportCtrl', ['$scope', '$http', '$routeParams', function($scope, $http, $routeParams) {
	// init
	$scope.error = null;
	$scope.status = null;
	$scope.importing = false;
	$scope.extracting = false;
	$scope.extracted = 0;
	$scope.saved = 0;
	$scope.measure = null;
	$scope.measurements = [
			{"name": "Meals", "title": "Food/Drink: ", "endpoint": "/nudge/api/v.1.1/users/@me/meals"},
			{"name": "Moves", "title": "Movement: ", "endpoint": "/nudge/api/v.1.1/users/@me/moves"},
			{"name": "Sleep", "title": "Slept ", "endpoint": "/nudge/api/v.1.1/users/@me/sleeps"},
			{"name": "Workouts", "title": "Workout: ", "endpoint": "/nudge/api/v.1.1/users/@me/workouts"}
	];
	var baseUrl = "https://jawbone.com";
	var nodeUrl = "https://" + window.location.hostname + ":5000";

	// start the importing of records
	$scope.startImport = function() {
		if (!$scope.measure) {
			$scope.error = "No category selected.";
			return;
		}
		$scope.error = null;
		$scope.status = "Importing data from Jawbone...";
		$scope.importing = true;
		$scope.extracting = true;
		$scope.extracted = 0;
		$scope.saved = 0;
		importRecords(baseUrl + $scope.measure.endpoint);
	}
	
	// import records
	importRecords = function(endpoint) {
		$http.get(nodeUrl + "/oauth2/data/" + $routeParams.userId + "/" + $routeParams.appId + "/" + encodeURIComponent(endpoint)).
		success(function(response) {
			$scope.extracted += response.data.items.length;
			response.data.items.forEach(saveRecord);
			
			// if there is more data, get it
			if (response.data.links && response.data.links.next) {
				importRecords(baseUrl + response.data.links.next);
			} else {
				$scope.extracting = false;
			}
		}).
		error(function(err) {
			$scope.error = "Failed to import data: " + err;
			$scope.extracting = false;
			$scope.importing = false;
			$scope.status = null;
		});
	}
	
	// save a single record to the database
	saveRecord = function(record) {
		var data = {
				"data": JSON.stringify(record),
				"name": $scope.measure.title + record.title,
				"description": "jawbone up armband " + $scope.measure.title + record.title
		};
		$http.post("https://" + window.location.hostname + ":9000/" + $routeParams.userId + "/apps/" + $routeParams.appId + "/create", JSON.stringify(data)).
			success(function() {
				$scope.saved += 1;
				if (!$scope.extracting && !$scope.error && $scope.extracted === $scope.saved) {
					$scope.status = "Importing records finished.";
					$scope.importing = false;
				}
			}).
			error(function(err) { $scope.error = "Failed to save record to database: " + err; });
	}
	
}]);

jawboneUp.controller('DetailsCtrl', ['$scope', '$routeParams', function($scope, $routeParams) {
	// init
    $scope.loading = true;

    // parse Base64 encoded JSON record
    var record = JSON.parse(atob($routeParams.record));
    $scope.output = JSON.stringify(record, null, "\t");
    $scope.loading = false;
}]);