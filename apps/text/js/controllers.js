var textRecordControllers = angular.module('textRecordControllers', []);

textRecordControllers.controller('CreateCtrl', ['$scope', '$http', '$routeParams',
	function($scope, $http, $routeParams) {
		
		// init
		$scope.errors = {};
		var replyTo = atob($routeParams.replyTo);
		
		// controller functions
		$scope.validate = function() {
			$scope.loading = true;
			$scope.hasError = false;
			$scope.validateTitle();
			$scope.validateData();
			if(!$scope.errors.title && !$scope.errors.data) {
				$scope.submit()
			}
			$scope.loading = false;
		};
		
		$scope.validateTitle = function() {
			$scope.errors.title = null;
			if (!$scope.title) {
				$scope.errors.title = "No title provided";
			} else if ($scope.title.length > 50) {
				$scope.errors.title = "Title too long.";
			}
		};
		
		$scope.validateData = function() {
			$scope.errors.data = null;
			if (!$scope.data) {
				$scope.errors.data = "No data provided.";
			}
		};
		
		$scope.submit = function() {
			// construct json
			var record = {
				"data": { "title": $scope.title, "data": $scope.data },
				"name": $scope.title,
				"description": $scope.data
			};
			
			// submit to server
			$http({ method: "POST", url: replyTo, data: record }).
				success(function() {
					$scope.success = "Record created successfully.";
					$scope.error = null;
					$scope.title = null;
					$scope.data = null;
				}).
				error(function(err) {
					$scope.success = null;
					$scope.error = err.responseText;
				});
		};
		
	}]);

textRecordControllers.controller('DetailsCtrl', ['$scope', '$routeParams',
	function($scope, $routeParams) {
		
		// init
		$scope.loading = true;
		
		// parse Base64 encoded JSON record
		var record = JSON.parse(atob($routeParams.record));
		$scope.title = record.title;
		$scope.data = record.data;
		$scope.loading = false;
		
	}]);
