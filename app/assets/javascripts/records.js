var records = angular.module('records', []);
records.controller('RecordsCtrl', ['$scope', '$http', function($scope, $http) {
	
	// init
	$scope.error = null;
	$scope.apps = [];
	$scope.records = [];
	$scope.loadingSpaces = false;
	$scope.spaces = [];
	$scope.loadingCircles = false;
	$scope.circles = [];
	
	// fetch apps
	$http(jsRoutes.controllers.Apps.fetch()).
		success(function(data) { $scope.apps = data; }).
		error(function(err) { $scope.error = "Failed to load apps: " + err; });
	
	// fetch records
	$http(jsRoutes.controllers.Records.fetch()).
		success(function(data) { $scope.records = data; }).
		error(function(err) { $scope.error = "Failed to load records: " + err; });
	
	// go to record creation dialog
	$scope.createRecord = function(app) {
		window.location.href = jsRoutes.controllers.Records.create(app._id).url;
	}
	
	// show record details
	$scope.showDetails = function(record) {
		window.location.href = jsRoutes.controllers.GlobalSearch.show("record", record._id).url;
	}
	
	// check whether the user is the owner of the record
	$scope.isOwnerOf = function(userId, record) {
		return userId === record.owner;
	}
	
	// activate record (spaces or circles of this record are being looked at)
	$scope.activate = function(record) {
		_.each($scope.records, function(record) { record.active = false; });
		record.active = true;
	}
	
	// get active record
	getActiveRecord = function() {
		return _.find($scope.records, function(record) { return record.active; });
	}
	
	// load spaces
	$scope.loadSpaces = function() {
		if ($scope.spaces.length === 0) {
			$scope.loadingSpaces = true;
			$http(jsRoutes.controllers.Spaces.fetch()).
				success(function(data) {
					$scope.error = null;
					$scope.spaces = data;
					$scope.loadingSpaces = false;
					prepareSpaces();
				}).
				error(function(err) {
					$scope.error = "Failed to load spaces: " + err;
					$scope.loadingSpaces = false;
				});
		} else {
			prepareSpaces();
		}
	}
	
	// set checkbox variable 'checked' for spaces that contain the currently
	// active record
	prepareSpaces = function() {
		_.each($scope.spaces, function(space) { space.checked = false; });
		var activeRecord = getActiveRecord();
		var spacesWithRecord = _.filter($scope.spaces, function(space) { return _.contains(space.records, activeRecord._id); });
		_.each(spacesWithRecord, function(space) { space.checked = true; });
	}
	
	// load circles
	$scope.loadCircles = function() {
		if ($scope.circles.length === 0) {
			$scope.loadingCircles = true;
			$http(jsRoutes.controllers.Circles.fetch()).
				success(function(data) {
					$scope.error = null;
					$scope.circles = data;
					$scope.loadingCircles = false;
					prepareCircles();
				}).
				error(function(err) {
					$scope.error = "Failed to load circles: " + err;
					$scope.loadingCircles = false;
				});
		} else {
			prepareCircles();
		}
	}
	
	// set checkbox variable 'checked' for circles that the currently active
	// record is shared with
	prepareCircles = function() {
		_.each($scope.circles, function(circle) { circle.checked = false; });
		var activeRecord = getActiveRecord();
		var circlesWithRecord = _.filter($scope.circles, function(circle) { return _.contains(circle.shared, activeRecord._id); });
		_.each(circlesWithRecord, function(circle) { circle.checked = true; });
	}
	
	// update spaces for active record
	$scope.updateSpaces = function() {
		var activeRecord = getActiveRecord();
		var checkedSpaces = _.filter($scope.spaces, function(space) { return space.checked; });
		var spaceIds = _.map(checkedSpaces, function(space) { return space._id; });
		var data = {"spaces": spaceIds};
		$http.post(jsRoutes.controllers.Records.updateSpaces(activeRecord._id).url, data).
			success(function() {
				$scope.error = null;
				_.each($scope.spaces, function(space) {
					var index = space.records.indexOf(activeRecord._id);
					if (index > -1) {
						space.records.splice(index, 1);
					}
				});
				_.each(checkedSpaces, function(space) { space.records.push(activeRecord._id); });
			}).
			error(function(err) { $scope.error = "Failed to update spaces: " + err; });
	}
	
	// update circles for active record
	$scope.updateCircles = function() {
		var activeRecord = getActiveRecord();
		var circlesWithRecord = _.filter($scope.circles, function(circle) { return _.contains(circle.shared, activeRecord._id); });
		var circlesChecked = _.filter($scope.circles, function(circle) { return circle.checked; });
		var circleIdsWithRecord = _.map(circlesWithRecord, function(circle) { return circle._id; });
		var circleIdsChecked = _.map(circlesChecked, function(circle) { return circle._id; });
		var circleIdsStarted = _.difference(circleIdsChecked, circleIdsWithRecord);
		var circleIdsStopped = _.difference(circleIdsWithRecord, circleIdsChecked);
		var data = {"started": circleIdsStarted, "stopped": circleIdsStopped};
		$http.post(jsRoutes.controllers.Records.updateSharing(activeRecord._id).url, data).
			success(function() {
				$scope.error = null;
				_.each($scope.circles, function(circle) {
					if (_.contains(circleIdsStarted, circle._id)) {
						circle.shared.push(activeRecord._id);
					} else if (_.contains(circleIdsStopped, circle._id)) {
						circle.shared.splice(circle.shared.indexOf(activeRecord._id), 1);
					}
				});
			}).
			error(function(err) { $scope.error = "Failed to update circles: " + err; });
	}
	
}]);