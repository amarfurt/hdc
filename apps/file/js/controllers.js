var fileRecordControllers = angular.module('fileRecordControllers', []);

fileRecordControllers.controller('CreateCtrl', ['$scope', '$http', '$routeParams',
	function($scope, $http, $routeParams) {
		
		// init
		$scope.errors = {};
		var replyTo = atob($routeParams.replyTo);
		
		// controller functions
		$scope.validate = function() {
			$scope.loading = true;
			$scope.hasError = false;
			$scope.validateTitle();
			$scope.validateFile();
			if(!$scope.errors.title && !$scope.errors.file) {
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
		
		$scope.validateFile = function() {
			$scope.errors.file = null;
			if (!$scope.file) {
				$scope.errors.file = "No file provided.";
			}
		};
		
		$scope.submit = function() {
			// construct json
            var r = new FileReader();
            r.onload = function(e) {
                var data = e.target.result;
                var record = {
                    "data": JSON.stringify({"title": $scope.title, "data": data}),
                    "name": $scope.title,
                    "description": data
                };
                
                // submit to server
                $http.post(replyTo, record).
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
            r.readAsText($scope.file);
        };
	}]);

fileRecordControllers.controller('DetailsCtrl', ['$scope', '$routeParams',
	function($scope, $routeParams) {
		
		// init
		$scope.loading = true;
		
		// parse Base64 encoded JSON record
		$scope.record = JSON.parse(atob($routeParams.record));
		$scope.loading = false;
		
	}]);
