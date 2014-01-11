var spaces = angular.module('spaces', []);
spaces.controller('SpacesCtrl', ['$scope', '$http', '$sce', '$filter', function($scope, $http, $sce, $filter) {
	
	// init
	$scope.error = null;
	$scope.spaces = [];
	$scope.add = {};
	$scope.loadingVisualizations = false;
	$scope.visualizations = [];
	$scope.searching = false;
	var activeSpace = null; // for filters
	
	// fetch spaces and make given space active (if one is given; first otherwise)
	$http(jsRoutes.controllers.Spaces.fetch()).
		success(function(data) {
			$scope.spaces = data;
			if ($scope.spaces.length > 0) {
				var active = window.location.pathname.split("/")[2];
				if (active) {
					$scope.makeActive(_.find($scope.spaces, function(space) { return space._id === active; }));
				} else {
					$scope.makeActive($scope.spaces[0]);
				}
			}
		}).
		error(function(err) { $scope.error = "Failed to load spaces: " + err; });
	
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
			loadBaseUrl(space);
		} else {
			spaceChanged(space);
		}
	}
	
	// load visualization url for given space
	loadBaseUrl = function(space) {
		$http(jsRoutes.controllers.Visualizations.getUrl(space.visualization)).
			success(function(url) {
				$scope.error = null;
				space.baseUrl = url;
				loadBaseRecords(space); // chain because callback is async
			}).
			error(function(err) { $scope.error = "Failed to load space '" + space.name + "': " + err; });
	}
	
	// load records for given space
	loadBaseRecords = function(space) {
		var data = {"records": space.records};
		$http.post(jsRoutes.controllers.Records.get().url, data).
			success(function(data) {
				$scope.error = null;
				space.baseRecords = data;
				spaceChanged(space); // chain because callback is async
			}).
			error(function(err) { $scope.error = "Failed to load records for space '" + space.name + "': " + err; });
	}
	
	// either the records of a space have changed or another space became active
	spaceChanged = function(space) {
		initFilters(space);
		reloadSpace(space);
	}
	
	// reload the space
	reloadSpace = function(space) {
		activeSpace = space; // active space for filters (works for compare copy as well)
		space.filteredRecords = $filter("filter")(space.baseRecords, matchesFilters);
		space.completedUrl = space.baseUrl.replace(":records", btoa(JSON.stringify(space.filteredRecords)));
		space.trustedUrl = $sce.trustAsResourceUrl(space.completedUrl);
		$("#iframe-" + space._id).attr("src", space.trustedUrl);
	}
	
	// *** FILTERS ***
	// initialize filters
	initFilters = function(space) {
		// app and owner
		space.select = {};
		if (space.baseRecords.length > 0) {
			space.select.apps = _.uniq(_.map(space.baseRecords, function(record) { return record.app; }));
			space.select.owners = _.uniq(_.map(space.baseRecords, function(record) { return record.owner; }));
		}
		$("#appFilter-" + space._id).on("change", function(event) { reloadSpace(space); });
		$("#ownerFilter-" + space._id).on("change", function(event) { reloadSpace(space); });
		
		// date
		space.filters = {};
		space.filters.date = "any";
		if (space.baseRecords.length > 0) {
			var sortedRecords = _.sortBy(space.baseRecords, "created");
			var earliest = _.first(sortedRecords);
			var latest = _.last(sortedRecords);
			earliest = stringToDate(earliest.created);
			latest = stringToDate(latest.created);
		} else {
			earliest = new Date();
			latest = new Date();
		}
		day = 1000 * 60 * 60 * 24;
		// TODO slider does not take added records into account once it is created...
		$("#dateFilter-" + space._id).slider({
			min:earliest.getTime(), max:latest.getTime() + day, step:day,
			value:[earliest.getTime(), latest.getTime() + day],
			formater: function(date) { return dateToString(new Date(date)); }
		}).
		slider("setValue", [earliest.getTime(), latest.getTime() + day]).
		on("slideStop", function(event) {
			var split = $("#dateFilter-" + space._id).val().split(",");
			space.filters.fromDate = Number(split[0]);
			space.filters.toDate = Number(split[1]);
			var fromDate = dateToString(new Date(space.filters.fromDate));
			var toDate = dateToString(new Date(space.filters.toDate));
			$scope.$apply(function() { space.filters.date = fromDate + " and " + toDate; });
			
			// reload the space
			reloadSpace(space);
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
	matchesFilters = function(record) {
		var space = activeSpace;
		if (space.filters.appId) {
			if (space.filters.appId !== record.app) {
				return false;
			}
		}
		if (space.filters.ownerId) {
			if (space.filters.ownerId !== record.owner) {
				return false;
			}
		}
		if (space.filters.fromDate && space.filters.toDate) {
			var recordDate = Number(stringToDate(record.created));
			if (space.filters.fromDate > recordDate || recordDate > space.filters.toDate) {
				return false;
			}
		}
		return true;
	}
	// *** FILTERS END ***
	
	// *** COMPARE ***
	$scope.startCompare = function(space) {
		// copy relevant properties
		space.copy = {};
		space.copy._id = "copy-" + space._id;
		space.copy.baseUrl = space.baseUrl;
		space.copy.baseRecords = space.baseRecords;
		
		// init filters and reload space
		spaceChanged(space.copy);
		
		// switch to compare mode
		space.compare = true;
		$(".slider").attr("style", "width:250px");
	}
	
	$scope.endCompare = function(space) {
		space.compare = false;
		space.copy = {};
		$(".slider").attr("style", "width:500px");
	}
	// *** COMPARE END ***
	
	// load all installed visualizations (for creating a new space)
	$scope.loadVisualizations = function() {
		if ($scope.visualizations.length === 0) {
			$scope.loadingVisualizations = true;
			$http(jsRoutes.controllers.Visualizations.fetch()).
				success(function(data) {
					$scope.error = null;
					$scope.visualizations = data;
					$scope.loadingVisualizations = false;
				}).
				error(function(err) {
					$scope.error = "Failed to load visualizations: " + err;
					$scope.loadingVisualizations = false;
				});
		}
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
		var data = {"name": $scope.add.name, "visualization": $scope.add.visualization};
		$http.post(jsRoutes.controllers.Spaces.add().url, data).
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
		$http(jsRoutes.controllers.Spaces["delete"](space._id)).
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
		return !_.contains(activeSpace.records, record._id);
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
		$http.post(jsRoutes.controllers.Spaces.addRecords(space._id).url, data).
			success(function() {
				$scope.error = null;
				$scope.foundRecords = [];
				_.each(recordIds, function(recordId) { space.records.push(recordId); });
				_.each(recordsToAdd, function(record) { space.baseRecords.push(record); });
				spaceChanged(space);
			}).
			error(function(err) { $scope.error = "Failed to add records: " + err; });
	}
	
}]);