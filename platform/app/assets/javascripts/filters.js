var filters = angular.module('filters', []);
filters.factory('filterData', function() {
	// service to share data between the record filter and the filter service
	var context = null;
	var filters = {};
	
	initData = function(_context, _serviceId) {
		this.context = _context;
		filters[_serviceId] = {};
		filters[_serviceId].current = [];
	}
	
	return {
		context: context,
		filters: filters,
		initData: initData
	}
	
});
filters.filter('recordFilter', ['filterData', function(filterData) {
	// custom filter for AngularJS
	return function(records, serviceId) {
		if (!filterData.filters[serviceId]) {
			return [];
		} else {
			return _.filter(records, function(record) {
				return _.every(filterData.filters[serviceId].current, function(filter) {
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
			});
		}
	}
}]);
filters.factory('filterService', ['$rootScope', '$http', '$q', 'filterData', function($rootScope, $http, $q, filterData) {
	// main service with a generic record filter
	
	// init
	var error = null;
	var records = [];
	var userId = null;
	var onChangeFunction = null;
	
	// initialize the service
	initService = function(_context, _serviceId, _records, _userId, _onChangeFunction) {
		filterData.initData(_context, _serviceId);
		records = _records;
		userId = _userId;
		onChangeFunction = _onChangeFunction;
	}
	
	// initialize filters once
	initFilters = function(serviceId) {
		if (!filterData.filters[serviceId].properties) {
			filterData.filters[serviceId].properties = [
				{"name": "app", "type": "point"},
				{"name": "owner", "type": "point"},
				{"name": "creator", "type": "point"},
				{"name": "created", "type": "range", "from": getMin("created"), "to": getMax("created")}
			];
			getPropertyValues("app", jsRoutes.controllers.Apps.get().url).then(
					function(values) { filterData.filters[serviceId].properties[0].values = values; }, 
					function(err) { error = err; });
			getPropertyValues("owner", jsRoutes.controllers.Users.get().url).then(
					function(values) { filterData.filters[serviceId].properties[1].values = renameCurUser(values); }, 
					function(err) { error = err; });
			getPropertyValues("creator", jsRoutes.controllers.Users.get().url).then(
					function(values) { filterData.filters[serviceId].properties[2].values = renameCurUser(values); }, 
					function(err) { error = err; });
		}
	}
	
	// get the min of all values of a property with a 'value' field
	getMin = function(property) {
		return _.min(records, function(record) { return record[property].value; })[property];
	}
	
	// get the max of all values of a property with a 'value' field
	getMax = function(property) {
		return _.max(records, function(record) { return record[property].value; })[property];
	}
	
	// get the values of a property, fetch the name from the given url and sort by it
	getPropertyValues = function(property, url) {
		var ids = _.uniq(_.pluck(records, property), false, function(value) { return value.$oid; });
		var data = {"properties": {"_id": ids}, "fields": ["name"]};
		var deferred = $q.defer();
		$http.post(url, JSON.stringify(data)).
			success(function(values) { deferred.resolve(_.sortBy(values, "name")); }).
			error(function(err) { deferred.reject("Failed to load filter values for property '" + property + "': " + err); });
		return deferred.promise;
	}
	
	// get current user (if present), rename as "myself" and put on top of the list
	renameCurUser = function(users) {
		var curUser = _.find(users, function(user) { return user._id.$oid === userId.$oid; });
		if (curUser) {
			curUser.name = "myself";
			users = _.union([curUser], _.without(users, curUser));
		}
		return users;
	}
	
	// add a new filter
	addFilter = function(serviceId) {
		initFilters(serviceId);
		filterData.filters[serviceId].current.push({"id": _.uniqueId("filter"), "context": filterData.context, "serviceId": serviceId});
	}
	
	// initialize slider (cannot be done in 'addFilter' since id of HTML slider element must be updated first)
	initSlider = function(filter) {
		if (!filter.sliderReady) {
			// hard code this metric and property for now
			var day = 1000 * 60 * 60 * 24;
			var property = "created";
			filter.from = getMin(property);
			filter.to = getMax(property);
			$("#" + filter.context + "-" + filter.serviceId).find("#" + filter.id).slider({range: true, min: filter.from.value.getTime(),
				max: filter.to.value.getTime(), step: day, values: [filter.from.value.getTime(), filter.to.value.getTime()],
				slide: function(event, ui){ sliderChanged(event, ui, filter); }});
			filter.sliderReady = true;
		}
	}
	
	// slider value changed
	sliderChanged = function(event, ui, filter) {
		$rootScope.$apply(function(){
			filter.from = {"name": dateToString(new Date(ui.values[0])), "value": new Date(ui.values[0])};
			filter.to = {"name": dateToString(new Date(ui.values[1])), "value": new Date(ui.values[1])};
		});
		onChangeFunction(filter.serviceId);
	}
	
	// remove a filter
	removeFilter = function(filter) {
		filterData.filters[filter.serviceId].current = _.without(filterData.filters[filter.serviceId].current, filter);
		onChangeFunction(filter.serviceId);
	}
	
	// convert date to string
	dateToString = function(date) {
		var year = date.getFullYear();
		var month = ((date.getMonth() < 9) ? "0" : "") + (date.getMonth() + 1);
		var day = ((date.getDate() < 10) ? "0" : "") + date.getDate();
		return year + "-" + month + "-" + day;
	}
	
	// api of the filter service
	return {
		// data structures
		filters: filterData.filters,
		error: error,
		// functions
		init: initService,
		initSlider: initSlider,
		addFilter: addFilter,
		removeFilter: removeFilter
	}
}]);