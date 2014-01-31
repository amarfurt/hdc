var controllers = angular.module('runAggregatorCtrls', []);

controllers.controller('RunAggregatorCtrl', ['$scope', '$routeParams',
	function($scope, $routeParams) {
		
		// init
		$scope.loading = true;
		$scope.distance = 0;
		$scope.time = 0;
		$scope.speed = 0;
		$scope.title = null;
		
		// parse Base64 encoded JSON records
		var records = JSON.parse(atob($routeParams.records));
		var users = [];
		for (var i = 0; i < records.length; i++) {
			users.push(records[i].owner);
			var data = JSON.parse(records[i].data).data;
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
		
		// display users if there are less than 3
		users = _.uniq(users, false, function(user) { return user.$oid; });
		if (0 < users.length && users.length <= 3) {
			if (users.length === 1) {
				$scope.title = "Runner: " + users[0].$oid;
			} else {
				$scope.title = "Runners: ";
				for (var i = 0; i < users.length; i++) {
					$scope.title = $scope.title.concat(users[i].$oid + ", ");
				}
				$scope.title = $scope.title.substring(0, $scope.title.length - 2);
			}
		} else {
			$scope.title = "Statistics of " + users.length + " runners";
		}
		$scope.loading = false;
	}]);
