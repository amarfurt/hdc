var controllers = angular.module('runAggregatorCtrls', []);

controllers.controller('RunAggregatorCtrl', ['$scope', '$http', '$routeParams',
	function($scope, $http, $routeParams) {
		
		// init
		$scope.loading = true;
		$scope.error = null;
		$scope.distance = 0;
		$scope.time = 0;
		$scope.speed = 0;
		
		// get the preprocessed metrics from the node server
		$http.get("https://" + window.location.hostname + ":5000/run-aggregator/" + $routeParams.cacheId).
			success(function(metrics) {
				$scope.distance = metrics.distance.toFixed(2);
				$scope.time = metrics.time.toFixed(2);
				$scope.speed = metrics.speed.toFixed(2);
				$scope.loading = false;
			}).
			error(function(err) { $scope.error = "Failed to load records: " + err; });

}]);
