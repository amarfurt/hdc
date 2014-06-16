var controllers = angular.module('recordListControllers', []);

controllers.controller('RecordListCtrl', ['$scope', '$http', '$routeParams',
	function($scope, $http, $routeParams) {
		
		// init
		$scope.loading = true;
		$scope.error = null;
		
		// get the records from the node server
		$http.get("https://" + window.location.hostname + ":5000/record-list/" + $routeParams.cacheId).
			success(function(records) {
				$scope.records = records;
				
				// assign ids for collapsible titles
				for (var i = 0; i < $scope.records.length; i++) {
					$scope.records[i].id = i;
				}
				
				// display the records
				$scope.loading = false;
			}).
			error(function(err) { $scope.error = "Failed to load records: " + err; });
		
	}]);
