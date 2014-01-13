var controllers = angular.module('recordListControllers', []);

controllers.controller('RecordListCtrl', ['$scope', '$routeParams',
	function($scope, $routeParams) {
		
		// init
		$scope.loading = true;
		
		// parse Base64 encoded JSON records
		$scope.records = JSON.parse(atob($routeParams.records));
		$scope.loading = false;

	}]);
