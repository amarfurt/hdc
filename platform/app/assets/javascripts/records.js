var records = angular.module('records', ['filters']);
records.controller('RecordsCtrl', ['$scope', '$http', 'filterService', function($scope, $http, filterService) {
	
	// init
	$scope.error = null;
	$scope.loadingApps = true;
	$scope.loadingRecords = true;
	$scope.userId = null;
	$scope.apps = [];
	$scope.records = [];
	$scope.loadingSpaces = false;
	$scope.spaces = [];
	$scope.loadingCircles = false;
	$scope.circles = [];
	
	// get current user
	$http(jsRoutes.controllers.Users.getCurrentUser()).
		success(function(userId) {
			$scope.userId = userId;
			getApps(userId);
			getRecords(userId);
		});
	
	// get apps
	getApps = function(userId) {
		var properties = {"_id": userId};
		var fields = ["apps"];
		var data = {"properties": properties, "fields": fields};
		$http.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data)).
			success(function(users) {
				getAppNames(users[0].apps);
			}).
			error(function(err) { $scope.error = "Failed to load apps: " + err; });
	}
	
	// get name for app ids
	getAppNames = function(appIds) {
		var properties = {"_id": appIds};
		var fields = ["name"];
		var data = {"properties": properties, "fields": fields};
		$http.post(jsRoutes.controllers.Apps.get().url, JSON.stringify(data)).
			success(function(apps) {
				$scope.apps = apps;
				$scope.loadingApps = false;
			}).
			error(function(err) { $scope.error = "Failed to load apps: " + err; });
	}
	
	// get records
	getRecords = function(userId) {
		$http(jsRoutes.controllers.Records.getVisibleRecords()).
			success(function(data) {
				$scope.records = data;
				prepareRecords();
				initFilterService($scope.records, userId);
				$scope.loadingRecords = false;
			}).
			error(function(err) { $scope.error = "Failed to load records: " + err; });
	}
	
	// prepare records: clip time from created and add JS date
	prepareRecords = function() {
		_.each($scope.records, function(record) {
			var date = record.created.split(" ")[0];
			var split = _.map(date.split("-"), function(num) { return Number(num); });
			record.created = {"name": date, "value": new Date(split[0], split[1] - 1, split[2])}
		});
	}
	
	// initialize filter service
	initFilterService = function(records, userId) {
		$scope.filterChanged = function(serviceId) { /* do nothing, automatically updated by filters */ }
		var context = "records";
		var serviceId = 0;
		filterService.init(context, serviceId, records, userId, $scope.filterChanged);
		$scope.filters = filterService.filters;
		$scope.filterError = filterService.error;
		$scope.addFilter = filterService.addFilter;
		$scope.removeFilter = filterService.removeFilter;
		
		// set filters if any are defined in the url
		if (window.location.pathname.indexOf("filters") !== -1) {
			var split = window.location.pathname.split("/");
			var name = split[3];
			var arg1 = split[4];
			var arg2 = split[5];
			$scope.addFilter(serviceId);
			var filter = _.last($scope.filters[serviceId].current);
			filter.property = _.findWhere($scope.filters[serviceId].properties, {"name": name});
			if (filter.property.type === "point") {
				filter.operator = arg1;
				if (arg1 === "is") {
					filter.operator = "";
				}
				filter.property.promise.then(function(values) {
					filter.value = _.find(values, function(value) { return value._id.$oid === arg2; });
				});
			} else if (filter.property.type === "range") {
				var split = _.map(arg1.split("-"), function(num) { return Number(num); });
				filter.from = {"name": arg1, "value": new Date(split[0], split[1] - 1, split[2])};
				split = _.map(arg2.split("-"), function(num) { return Number(num); });
				filter.to = {"name": arg2, "value": new Date(split[0], split[1] - 1, split[2])};
				filterService.setSlider(filter, filter.from, filter.to);
			}
		}
	}
	
	// go to record creation dialog
	$scope.createRecord = function(app) {
		window.location.href = jsRoutes.controllers.Records.create(app._id.$oid).url;
	}
	
	// show record details
	$scope.showDetails = function(record) {
		window.location.href = jsRoutes.controllers.Records.details(record._id.$oid).url;
	}
	
	// check whether the user is the owner of the record
	$scope.isOwnRecord = function(record) {
		return $scope.userId.$oid === record.owner.$oid;
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
			var properties = {"owner": $scope.userId};
			var fields = ["name", "records", "order"];
			var data = {"properties": properties, "fields": fields};
			$http.post(jsRoutes.controllers.Spaces.get().url, JSON.stringify(data)).
				success(function(spaces) {
					$scope.error = null;
					$scope.spaces = spaces;
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
		var spacesWithRecord = _.filter($scope.spaces, function(space) { return containsRecord(space.records, activeRecord._id); });
		_.each(spacesWithRecord, function(space) { space.checked = true; });
	}
	
	// load circles
	$scope.loadCircles = function() {
		if ($scope.circles.length === 0) {
			$scope.loadingCircles = true;
			var properties = {"owner": $scope.userId};
			var fields = ["name", "shared", "order"];
			var data = {"properties": properties, "fields": fields};
			$http.post(jsRoutes.controllers.Circles.get().url, JSON.stringify(data)).
				success(function(circles) {
					$scope.error = null;
					$scope.circles = circles;
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
		var circlesWithRecord = _.filter($scope.circles, function(circle) { return containsRecord(circle.shared, activeRecord._id); });
		_.each(circlesWithRecord, function(circle) { circle.checked = true; });
	}
	
	// helper method for contains
	containsRecord = function(recordIdList, recordId) {
		var ids = _.map(recordIdList, function(element) { return element.$oid; });
		return _.contains(ids, recordId.$oid);
	}
	
	// update spaces for active record
	$scope.updateSpaces = function() {
		var activeRecord = getActiveRecord();
		var checkedSpaces = _.filter($scope.spaces, function(space) { return space.checked; });
		var spaceIds = _.map(checkedSpaces, function(space) { return space._id; });
		var data = {"spaces": spaceIds};
		$http.post(jsRoutes.controllers.Records.updateSpaces(activeRecord._id.$oid).url, JSON.stringify(data)).
			success(function() {
				$scope.error = null;
				_.each($scope.spaces, function(space) {
					removeRecordIfPresent(space.records, activeRecord._id);
				});
				_.each(checkedSpaces, function(space) { space.records.push(activeRecord._id); });
			}).
			error(function(err) { $scope.error = "Failed to update spaces: " + err; });
	}
	
	// helper method for remove (in cases where object equality doesn't work)
	removeRecordIfPresent = function(recordIdList, recordId) {
		_.each(recordIdList, function(element) {
			if (element.$oid === recordId.$oid) {
				recordIdList.splice(recordIdList.indexOf(element));
			}
		});
	}
	
	// update circles for active record
	$scope.updateCircles = function() {
		var activeRecord = getActiveRecord();
		var circlesWithRecord = _.filter($scope.circles, function(circle) { return containsRecord(circle.shared, activeRecord._id); });
		var circlesChecked = _.filter($scope.circles, function(circle) { return circle.checked; });
		var circleIdsWithRecord = _.map(circlesWithRecord, function(circle) { return circle._id.$oid; });
		var circleIdsChecked = _.map(circlesChecked, function(circle) { return circle._id.$oid; });
		var idsStarted = _.difference(circleIdsChecked, circleIdsWithRecord);
		var idsStopped = _.difference(circleIdsWithRecord, circleIdsChecked);
		// construct objectId objects again...
		var circleIdsStarted = _.map(idsStarted, function(id) { return {"$oid": id}; });
		var circleIdsStopped = _.map(idsStopped, function(id) { return {"$oid": id}; });
		var data = {"started": circleIdsStarted, "stopped": circleIdsStopped};
		$http.post(jsRoutes.controllers.Records.updateSharing(activeRecord._id.$oid).url, JSON.stringify(data)).
			success(function() {
				$scope.error = null;
				_.each($scope.circles, function(circle) {
					if (containsRecord(circleIdsStarted, circle._id)) {
						circle.shared.push(activeRecord._id);
					} else if (containsRecord(circleIdsStopped, circle._id)) {
						removeRecordIfPresent(circle.shared, activeRecord._id);
					}
				});
			}).
			error(function(err) { $scope.error = "Failed to update circles: " + err; });
	}
	
}]);

// record creation
var createRecords = angular.module('createRecords', []);
createRecords.controller('CreateRecordsCtrl', ['$scope', '$http', '$sce', function($scope, $http, $sce) {
	
	// init
	$scope.error = null;
	
	// get app id (format: /records/create/:appId)
	var appId = window.location.pathname.split("/")[3];
	
	// get record creation url
	$http(jsRoutes.controllers.Apps.getCreateUrl(appId)).
		success(function(url) {
			$scope.error = null;
			$scope.url = $sce.trustAsResourceUrl(url);
		}).
		error(function(err) { $scope.error = "Failed to load record creation dialog: " + err; });
	
}]);