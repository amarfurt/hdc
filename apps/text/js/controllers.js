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
			$scope.validateContent();
			if(!$scope.errors.title && !$scope.errors.content) {
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
		
		$scope.validateContent = function() {
			$scope.errors.content = null;
			if (!$scope.content) {
				$scope.errors.content = "No content provided.";
			}
		};
		
		$scope.submit = function() {
			// construct json
			var record = {
				"data": JSON.stringify({"title": $scope.title, "content": $scope.content}),
				"name": $scope.title,
				"description": $scope.content
			};
			
			// submit to server
			$http.post(replyTo, record).
				success(function() {
					$scope.success = "Record created successfully.";
					$scope.error = null;
					$scope.title = null;
					$scope.content = null;
				}).
				error(function(err) {
					$scope.success = null;
					$scope.error = err;
				});
		};
		
	}]);

textRecordControllers.controller('DetailsCtrl', ['$scope', '$routeParams',
	function($scope, $routeParams) {
		
		// init
		$scope.loading = true;
		
		// parse Base64 encoded JSON record
		$scope.record = JSON.parse($routeParams.record);
		$scope.loading = false;
		
	}]);
