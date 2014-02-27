var records = angular.module('records', []);
records.controller('RecordsCtrl', ['$scope', '$http', '$q', function($scope, $http, $q) {
	
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
	
	// --- FILTERS ---
	$scope.filters = {};
	$scope.filters.current = [];
	// --- FILTERS END ---
	
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
				$scope.loadingRecords = false;
			}).
			error(function(err) { $scope.error = "Failed to load records: " + err; });
	}
	
	// prepare records: clip time from created and add JS date
	prepareRecords = function() {
		_.each($scope.records, function(record) {
			var date = record.created.split(" ")[0];
			var split = _.map(date.split(/[ -]/), function(num) { return Number(num); });
			record.created = {"name": date, "value": new Date(split[0], split[1] - 1, split[2])}
		});
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
	
	// --- FILTERS ---
	// requires $q to be injected
	// initialize filters once
	initFilters = function() {
		if (!$scope.filters.properties) {
			$scope.filters.properties = [
				{"name": "app", "type": "point"},
				{"name": "owner", "type": "point"},
				{"name": "creator", "type": "point"},
				{"name": "created", "type": "range", "from": getMin("created"), "to": getMax("created")}
			];
			getPropertyValues("app", jsRoutes.controllers.Apps.get().url).then(
					function(values) { $scope.filters.properties[0].values = values; }, 
					function(err) { $scope.error = err; });
			getPropertyValues("owner", jsRoutes.controllers.Users.get().url).then(
					function(values) { $scope.filters.properties[1].values = renameCurUser(values); }, 
					function(err) { $scope.error = err; });
			getPropertyValues("creator", jsRoutes.controllers.Users.get().url).then(
					function(values) { $scope.filters.properties[2].values = renameCurUser(values); }, 
					function(err) { $scope.error = err; });
		}
	}
	
	// get the min of all values of a property with a 'value' field
	getMin = function(property) {
		return _.min($scope.records, function(record) { return record[property].value; })[property];
	}
	
	// get the max of all values of a property with a 'value' field
	getMax = function(property) {
		return _.max($scope.records, function(record) { return record[property].value; })[property];
	}
	
	// get the values of a property, fetch the name from the given url and sort by it
	getPropertyValues = function(property, url) {
		var ids = _.uniq(_.pluck($scope.records, property), false, function(value) { return value.$oid; });
		var data = {"properties": {"_id": ids}, "fields": ["name"]};
		var deferred = $q.defer();
		$http.post(url, JSON.stringify(data)).
			success(function(values) { deferred.resolve(_.sortBy(values, "name")); }).
			error(function(err) { deferred.reject("Failed to load filter values for property '" + property + "': " + err); });
		return deferred.promise;
	}
	
	// get current user (if present), rename as "myself" and put on top of the list
	renameCurUser = function(users) {
		var curUser = _.find(users, function(user) { return user._id.$oid === $scope.userId.$oid; });
		if (curUser) {
			curUser.name = "myself";
			users = _.union([curUser], _.without(users, curUser));
		}
		return users;
	}
	
	// add a new filter
	$scope.addFilter = function() {
		initFilters();
		$scope.filters.current.push({"id": _.uniqueId("filter")});
	}
	
	// initialize slider (cannot be done in 'addFilter' since id of HTML slider element must be updated first)
	$scope.initSlider = function(filter) {
		if (!filter.sliderReady) {
			// hard code this metric and property for now
			var day = 1000 * 60 * 60 * 24;
			var property = "created";
			filter.from = getMin(property);
			filter.to = getMax(property);
			$("#" + filter.id).slider({range: true, min: filter.from.value.getTime(), max: filter.to.value.getTime(), step: day, 
				values: [filter.from.value.getTime(), filter.to.value.getTime()], slide: function(event, ui){ sliderChanged(event, ui, filter); }});
			filter.sliderReady = true;
		}
	}
	
	// slider value changed
	sliderChanged = function(event, ui, filter) {
		$scope.$apply(function(){
			filter.from = {"name": dateToString(new Date(ui.values[0])), "value": new Date(ui.values[0])};
			filter.to = {"name": dateToString(new Date(ui.values[1])), "value": new Date(ui.values[1])};
		});
	}
	
	// remove a filter
	$scope.removeFilter = function(filter) {
		$scope.filters.current = _.without($scope.filters.current, filter);
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
		return _.every($scope.filters.current, function(filter) {
			if (!filter.property) {
				return true;
			} else if (filter.property.type === "point" && filter.value) {
				if (!filter.operator) { // equality
					return record[filter.property.name].$oid === filter.value._id.$oid;
				} else if (filter.operator === "not") { // inequality
					return record[filter.property.name].$oid !== filter.value._id.$oid;
				}
			} else if (filter.sliderReady && filter.property.type === "range") {
				return filter.from.value <= record[filter.property.name].value && record[filter.property.name].value <= filter.to.value;
			}
			return true;
		});
	}
	// --- FILTERS END ---
	
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