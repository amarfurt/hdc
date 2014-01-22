var records = angular.module('records', []);
records.controller('RecordsCtrl', ['$scope', '$http', function($scope, $http) {
	
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
	$scope.filter = {};
	$scope.select = {};
	
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
				initFilters();
				$scope.loadingRecords = false;
			}).
			error(function(err) { $scope.error = "Failed to load records: " + err; });
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
	
	// *** filters ***
	// initialize filters
	initFilters = function() {
		// app and owner
		if ($scope.records.length > 0) {
			var appIds = _.uniq(_.map($scope.records, function(record) { return record.app.$oid; }));
			var ownerIds = _.uniq(_.map($scope.records, function(record) { return record.owner.$oid; }));
			// get the names
			var properties = {"_id": _.map(appIds, function(id) { return {"$oid": id}; })};
			var fields = ["name"];
			var data = {"properties": properties, "fields": fields};
			$http.post(jsRoutes.controllers.Apps.get().url, JSON.stringify(data)).
				success(function(apps) { $scope.select.apps = apps; });
			properties = {"_id": _.map(ownerIds, function(id) { return {"$oid": id}; })};
			data = {"properties": properties, "fields": fields};
			$http.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data)).
				success(function(users) { $scope.select.owners = users; });
		}
		
		// date
		$scope.filter.date = "any";
		if ($scope.records.length > 0) {
			var sortedRecords = _.sortBy($scope.records, "created");
			var earliest = _.first(sortedRecords);
			var latest = _.last(sortedRecords);
			earliest = stringToDate(earliest.created);
			latest = stringToDate(latest.created);
		} else {
			earliest = new Date();
			latest = new Date();
		}
		day = 1000 * 60 * 60 * 24;
		$("#dateFilter").slider({
			min:earliest.getTime(), max:latest.getTime() + day, step:day,
			value:[earliest.getTime(), latest.getTime() + day],
			formater: function(date) { return dateToString(new Date(date)); }
		}).
		on("slideStop", function(event) {
			// rerun the filters (is not automatically triggered)
			$scope.$apply(function() {
				var split = $("#dateFilter").val().split(",");
				$scope.filter.fromDate = Number(split[0]);
				$scope.filter.toDate = Number(split[1]);
				var fromDate = dateToString(new Date($scope.filter.fromDate));
				var toDate = dateToString(new Date($scope.filter.toDate));
				$scope.filter.date = fromDate + " and " + toDate;
			});
		});
	}
	
	// convert date in string format to JS date
	stringToDate = function(dateString) {
		var split = dateString.split(/[ -]/);
		split = _.map(split, function(number) { return Number(number); });
		return new Date(split[0], split[1] - 1, split[2]);
	}
	
	// convert date to string
	dateToString = function(date) {
		var year = date.getFullYear();
		var month = ((date.getMonth() < 9) ? "0" : "") + (date.getMonth() + 1);
		var day = ((date.getDate() < 10) ? "0" : "") + date.getDate();
		return year + "-" + month + "-" + day;
	}
	
	// checks whether a record matches all filters
	$scope.matchesFilters = function(record) {
		if ($scope.filter.appId) {
			if ($scope.filter.appId.$oid !== record.app.$oid) {
				return false;
			}
		}
		if ($scope.filter.ownerId) {
			if ($scope.filter.ownerId.$oid !== record.owner.$oid) {
				return false;
			}
		}
		if ($scope.filter.fromDate && $scope.filter.toDate) {
			var recordDate = Number(stringToDate(record.created));
			if ($scope.filter.fromDate > recordDate || recordDate > $scope.filter.toDate) {
				return false;
			}
		}
		return true;
	}
	
}]);
records.controller('CreateRecordsCtrl', ['$scope', '$http', '$sce', function($scope, $http, $sce) {
	
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