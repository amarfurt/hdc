var search = angular.module('search', []);
search.controller('SearchCtrl', ['$scope', '$http', function($scope, $http) {
	
	// init
	$scope.error = null;
	$scope.loading = false;
	$scope.results = {};
	$scope.types = [];
	$scope.active = null;
	
	// get search query (format: /serach/:query)
	$scope.query = decodeURI(window.location.pathname.split("/")[2]);
	
	// start search
	$scope.loading = true;
	$http(jsRoutes.controllers.GlobalSearch.search($scope.query)).
		success(function(data) {
			$scope.error = null;
			$scope.results = data;
			$scope.types = Object.keys($scope.results);
			if ($scope.types.length > 0) {
				$scope.makeActive($scope.types[0]);
			}
			$scope.loading = false;
		}).
		error(function(err) {
			$scope.error = "Search failed: " + err;
			$scope.loading = false;
		});
	
	$scope.makeActive = function(type) {
		$scope.active = type;
		
	}
	
}]);