var spaces = angular.module('spaces', ['filters']);
spaces.controller('SpacesCtrl', ['$scope', '$http', '$sce', '$filter', 'filterService', function($scope, $http, $sce, $filter, filterService) {
	
	// init
	$scope.error = null;
	$scope.userId = null;
	$scope.loading = true;
	$scope.spaces = [];
	$scope.add = {};
	$scope.loadingVisualizations = false;
	$scope.visualizations = [];
	$scope.searching = false;
	
	// get current user
	$http(jsRoutes.controllers.Users.getCurrentUser()).
		success(function(userId) {
			$scope.userId = userId;
			getSpaces();
		});
	
	// get spaces and make given space active (if one is given; first otherwise)
	getSpaces = function(userId) {
		var properties = {"owner": $scope.userId};
		var fields = ["name", "records", "visualization", "order"]
		var data = {"properties": properties, "fields": fields};
		$http.post(jsRoutes.controllers.Spaces.get().url, JSON.stringify(data)).
			success(function(spaces) {
				$scope.spaces = spaces;
				if ($scope.spaces.length > 0) {
					var active = window.location.pathname.split("/")[2];
					if (active) {
						$scope.makeActive(_.find($scope.spaces, function(space) { return space._id.$oid === active; }));
					} else {
						$scope.makeActive($scope.spaces[0]);
					}
				}
				$scope.loading = false;
			}).
			error(function(err) {
				$scope.error = "Failed to load spaces: " + err;
				$scope.loading = false;
			});
	}
	
	// make space tab active
	$scope.makeActive = function(space) {
		_.each($scope.spaces, function(space) { space.active = false; });
		space.active = true;
		
		var compareActive = _.find($scope.spaces, function(space) { return space.compare; });
		if (compareActive) {
			$scope.endCompare(compareActive);
		}
	
		// load url, records and visualization
		if (!space.baseUrl) {
			space.serviceId = 0;
			loadBaseUrl(space);
		} else {
			spaceChanged(space);
		}
	}
	
	// load visualization url for given space
	loadBaseUrl = function(space) {
		$http(jsRoutes.controllers.Visualizations.getUrl(space.visualization.$oid)).
			success(function(url) {
				space.baseUrl = url;
				loadBaseRecords(space); // chain because callback is async
			}).
			error(function(err) { $scope.error = "Failed to load space '" + space.name + "': " + err; });
	}
	
	// load records for given space
	loadBaseRecords = function(space) {
		var properties = {"_id": space.records};
		var fields = ["app", "owner", "creator", "created", "name", "data"];
		var data = {"properties": properties, "fields": fields};
		$http.post(jsRoutes.controllers.Records.get().url, JSON.stringify(data)).
			success(function(records) {
				$scope.error = null;
				space.baseRecords = records;
				prepareRecords(space.baseRecords)
				spaceChanged(space); // chain because callback is async
			}).
			error(function(err) { $scope.error = "Failed to load records for space '" + space.name + "': " + err; });
	}
	
	// prepare records: clip time from created and add JS date
	prepareRecords = function(records) {
		_.each(records, function(record) {
			var date = record.created.split(" ")[0];
			var split = _.map(date.split(/[ -]/), function(num) { return Number(num); });
			record.created = {"name": date, "value": new Date(split[0], split[1] - 1, split[2])}
		});
	}
	
	// either the records of a space have changed or another space became active
	spaceChanged = function(space) {
		initFilterService(space, $scope.userId);
		reloadSpace(space);
	}
	
	// initialize filter service
	initFilterService = function(space, userId) {
		$scope.filterChanged = function(serviceId) {
			var activeSpace = _.find($scope.spaces, function(space) { return space.active; });
			if (serviceId === 0) {
				reloadSpace(activeSpace);
			} else {
				reloadSpace(activeSpace.copy);
			}
		}
		filterService.init(space.name, space.serviceId, space.baseRecords, userId, $scope.filterChanged);
		$scope.filters = filterService.filters;
		$scope.filterError = filterService.error;
		$scope.initSlider = filterService.initSlider;
		$scope.addFilter = filterService.addFilter;
		$scope.removeFilter = filterService.removeFilter;
	}
	
	// reload the space
	reloadSpace = function(space) {
		var filteredRecords = $filter("recordFilter")(space.baseRecords, space.serviceId);
		var filteredData = _.map(filteredRecords, function(record) { return record.data; });
		var completedUrl = space.baseUrl.replace(":records", btoa(JSON.stringify(filteredData)));
		space.trustedUrl = $sce.trustAsResourceUrl(completedUrl);
		$("#iframe-" + space._id.$oid).attr("src", space.trustedUrl);
	}
	
	// *** COMPARE ***
	$scope.startCompare = function(space) {
		// copy relevant properties
		space.copy = {};
		space.copy._id = {"$oid": "copy-" + space._id.$oid};
		space.copy.serviceId = 1;
		space.copy.name = space.name;
		space.copy.baseUrl = space.baseUrl;
		space.copy.baseRecords = space.baseRecords;
		
		// init filters and reload space
		spaceChanged(space.copy);
		
		// switch to compare mode
		space.compare = true;
	}
	
	$scope.endCompare = function(space) {
		space.compare = false;
		space.copy = {};
	}
	// *** COMPARE END ***
	
	// load all installed visualizations (for creating a new space)
	$scope.loadVisualizations = function() {
		if ($scope.visualizations.length === 0) {
			$scope.loadingVisualizations = true;
			var properties = {"_id": $scope.userId};
			var fields = ["visualizations"];
			var data = {"properties": properties, "fields": fields};
			$http.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data)).
				success(function(users) { getVisualizations(users[0].visualizations); }).
				error(function(err) {
					$scope.error = "Failed to load visualizations: " + err;
					$scope.loadingVisualizations = false;
				});
		}
	}
	
	getVisualizations = function(ids) {
		var properties = {"_id": ids};
		var fields = ["name"];
		var data = {"properties": properties, "fields": fields};
		$http.post(jsRoutes.controllers.Visualizations.get().url, JSON.stringify(data)).
			success(function(visualizations) {
				$scope.error = null;
				$scope.visualizations = visualizations;
				$scope.loadingVisualizations = false;
			}).
			error(function(err) {
				$scope.error = "Failed to load visualizations: " + err;
				$scope.loadingVisualizations = false;
			});
	}
	
	// add a space
	$scope.addSpace = function() {
		// dismiss modal
		$("#spaceModal").modal("hide");
		
		// check user input
		if (!$scope.add.name) {
			$scope.error = "Please provide a name for your new space.";
			return;
		} else if (!$scope.add.visualization) {
			$scope.error = "Please select a visualization for your new space.";
			return;
		}
		
		// send the request
		var data = {"name": $scope.add.name, "visualization": $scope.add.visualization.$oid};
		$http.post(jsRoutes.controllers.Spaces.add().url, JSON.stringify(data)).
			success(function(space) {
				$scope.error = null;
				$scope.add = {};
				$scope.spaces.push(space);
				$scope.makeActive(space);
			}).
			error(function(err) { $scope.error = "Failed to add space '" + data.name + "': " + err; });
	}
	
	// delete a space
	$scope.deleteSpace = function(space) {
		$http(jsRoutes.controllers.Spaces["delete"](space._id.$oid)).
			success(function() {
				$scope.error = null;
				$scope.spaces.splice($scope.spaces.indexOf(space), 1);
				if ($scope.spaces.length > 0) {
					$scope.spaces[0].active = true;
				}
			}).
			error(function(err) { $scope.error = "Failed to delete space '" + space.name + "': " + err; });
	}
	
	// check whether record is not already in active space
	$scope.isntInSpace = function(record) {
		var activeSpace = _.find($scope.spaces, function(space) { return space.active; });
		return !containsRecord(activeSpace.records, record._id);
	}
	
	// helper method for contains
	containsRecord = function(recordIdList, recordId) {
		var ids = _.map(recordIdList, function(element) { return element.$oid; });
		return _.contains(ids, recordId.$oid);
	}
	
	// search for records
	$scope.searchRecords = function(space) {
		$scope.searching = true;
		var query = space.recordQuery;
		if (query) {
		$http(jsRoutes.controllers.Records.search(query)).
			success(function(records) {
				$scope.error = null;
				$scope.foundRecords = records;
				$scope.searching = false;
			}).
			error(function(err) {
				$scope.error = "Record search failed: " + err;
				$scope.searching = false;
			});
		}
	}
	
	// add a record
	$scope.addRecords = function(space) {
		// get the ids of the records that should be added to the space
		var recordsToAdd = _.filter($scope.foundRecords, function(record) { return record.checked; });
		var recordIds = _.map(recordsToAdd, function(record) { return record._id; });
		
		var data = {"records": recordIds};
		$http.post(jsRoutes.controllers.Spaces.addRecords(space._id.$oid).url, JSON.stringify(data)).
			success(function() {
				$scope.error = null;
				space.recordQuery = null;
				$scope.foundRecords = [];
				_.each(recordIds, function(recordId) { space.records.push(recordId); });
				_.each(recordsToAdd, function(record) { space.baseRecords.push(record); });
				spaceChanged(space);
			}).
			error(function(err) { $scope.error = "Failed to add records: " + err; });
	}
	
}]);