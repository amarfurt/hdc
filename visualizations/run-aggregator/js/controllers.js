var controllers = angular.module('runAggregatorCtrls', []);

controllers.controller('RunAggregatorCtrl', ['$scope', '$routeParams',
	function($scope, $routeParams) {
		
		// init
		$scope.loading = true;
		$scope.distance = 0;
		$scope.time = 0;
		$scope.speed = 0;
		
		// parse Base64 encoded JSON records
		var records = JSON.parse(atob($routeParams.records));
		for (i = 0; i < records.length; i++) {
			var data = JSON.parse(records[i]).data;
			if (data) {
				var distanceEnd = data.lastIndexOf("km");
				var timeEnd = data.lastIndexOf("h");
				if (distanceEnd !== -1 && timeEnd !== -1) {
					var curDistance = data.substring(0, distanceEnd).trim();
					curDistance = curDistance.substring(curDistance.lastIndexOf(" ") + 1);
					var curTime = data.substring(0, timeEnd).trim();
					curTime = curTime.substring(curTime.lastIndexOf(" ") + 1);
					var distance = parseFloat(curDistance);
					var time = parseFloat(curTime);
					if (!isNaN(distance) && !isNaN(time)) {
						$scope.distance += distance;
						$scope.time += time;
					}
				}
			}
		}
		if ($scope.time > 0) {
			$scope.speed = $scope.distance / $scope.time;
		}
		$scope.distance = $scope.distance.toFixed(2);
		$scope.time = $scope.time.toFixed(2);
		$scope.speed = $scope.speed.toFixed(2);
		$scope.loading = false;

	}]);
