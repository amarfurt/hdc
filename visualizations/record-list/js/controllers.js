var controllers = angular.module('recordListControllers', []);

controllers.controller('RecordListCtrl', ['$scope', '$routeParams',
	function($scope, $routeParams) {
		
		// init
		$scope.loading = true;
		
		// parse Base64 encoded JSON records
		$scope.records = JSON.parse(atob($routeParams.records));
		
		// assign ids for collapsible titles
		for (var i = 0; i < $scope.records.length; i++) {
			$scope.records[i] = JSON.parse($scope.records[i]);
			$scope.records[i].id = i;
		}
		
		// display the records
		$scope.loading = false;
		
	}]);
