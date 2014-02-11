var news = angular.module('news', []);
news.controller('NewsCtrl', ['$scope', '$http', function($scope, $http) {
	
	// init
	$scope.error = null;
	$scope.loadingNews = true;
	$scope.loadingRecords = true;
	$scope.news = [];
	$scope.pushed = [];
	$scope.shared = [];
	$scope.newsItems = {};
	$scope.records = {};
	$scope.users = {};
	$scope.apps = {};
	$scope.pushs = {};
	$scope.pushs.show = false;
	$scope.shares = {};
	$scope.shares.show = false;
	
	// get current user
	$http(jsRoutes.controllers.Users.getCurrentUser()).
		success(function(userId) { getNews(userId); });
	
	// get user's news
	getNews = function(userId) {
		var properties = {"_id": userId};
		var fields = ["news", "pushed", "shared"];
		var data = {"properties": properties, "fields": fields};
		$http.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data)).
			success(function(users) {
				$scope.news = users[0].news;
				getNewsItems($scope.news);
				$scope.pushed = users[0].pushed;
				$scope.shared = users[0].shared;
				var recordIds = _.flatten([$scope.pushed, $scope.shared]);
				getRecords(recordIds);
				$("[rel='tooltip']").tooltip();
			}).
			error(function(err) {
				$scope.error = "Failed to load your news: " + err;
				$scope.loadingNews = false;
				$scope.loadingRecords = false;
			});
	}
	
	// get the news items
	getNewsItems = function(newsItemIds) {
		var properties = {"_id": newsItemIds};
		var fields = ["creator", "created", "title", "content"];
		var data = {"properties": properties, "fields": fields};
		$http.post(jsRoutes.controllers.News.get().url, JSON.stringify(data)).
			success(function(newsItems) {
				_.each(newsItems, function(newsItem) { $scope.newsItems[newsItem._id.$oid] = newsItem; });
				var creatorIds = _.pluck(newsItems, "creator");
				creatorIds = _.uniq(creatorIds, false, function(id) { return id.$oid; });
				getUserNames(creatorIds);
				$scope.loadingNews = false;
			}).
			error(function(err) {
				$scope.error = "Failed to load news items: " + err;
				$scope.loadingNews = false;
			});
	}
	
	// get the records
	getRecords = function(recordIds) {
		var properties = {"_id": recordIds};
		var fields = ["app", "owner", "creator", "created", "name"];
		var data = {"properties": properties, "fields": fields};
		$http.post(jsRoutes.controllers.Records.get().url, JSON.stringify(data)).
			success(function(records) { 
				_.each(records, function(record) { $scope.records[record._id.$oid] = record; });
				var creatorIds = _.pluck(records, "creator");
				var ownerIds = _.pluck(records, "owner");
				var userIds = _.union(creatorIds, ownerIds);
				userIds = _.uniq(userIds, false, function(id) { return id.$oid; });
				getUserNames(userIds);
				var appIds = _.pluck(records, "app");
				appIds = _.uniq(appIds, false, function(id) { return id.$oid; });
				getAppNames(appIds);
				$scope.loadingRecords = false;
			}).
			error(function(err) {
				$scope.error = "Failed to load records: " + err;
				$scope.loadingRecords = false;
			});
	}
	
	// get the user names
	getUserNames = function(userIds) {
		var properties = {"_id": userIds};
		var fields = ["name"];
		var data = {"properties": properties, "fields": fields};
		$http.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data)).
			success(function(users) { _.each(users, function(user) { $scope.users[user._id.$oid] = user; })}).
			error(function(err) { $scope.error = "Failed to load user names: " + err; });
	}
	
	// get the app names
	getAppNames = function(appIds) {
		var properties = {"_id": appIds};
		var fields = ["name"];
		var data = {"properties": properties, "fields": fields};
		$http.post(jsRoutes.controllers.Apps.get().url, JSON.stringify(data)).
			success(function(apps) { _.each(apps, function(app) { $scope.apps[app._id.$oid] = app; })}).
			error(function(err) { $scope.error = "Failed to load app names: " + err; });
	}
	
	// display the records that were pushed since the last login
	$scope.recordsPushed = function() {
		$scope.pushs.details = _.map($scope.pushed, function(recordId) { return $scope.records[recordId.$oid]; });
		$scope.shares.show = false;
		$scope.pushs.show = true;
	}
	
	// display the apps that pushed records
	$scope.appsPushed = function() {
		var appIds = _.map($scope.pushed, function(recordId) { return $scope.records[recordId.$oid].app; });
		appIds = _.uniq(appIds, false, function(id) { return id.$oid; });
		$scope.pushs.details = _.map(appIds, function(appId) { return $scope.apps[appId.$oid]; });
		$scope.shares.show = false;
		$scope.pushs.show = true;
	}
	
	// display the records that where shared with the user since the last login
	$scope.recordsShared = function() {
		$scope.shares.details = _.map($scope.shared, function(recordId) { return $scope.records[recordId.$oid]; });
		$scope.pushs.show = false;
		$scope.shares.show = true;
	}
	
	// display the users that shared records with the user
	$scope.usersShared = function() {
		var ownerIds = _.map($scope.shared, function(recordId) { return $scope.records[recordId.$oid].owner; });
		ownerIds = _.uniq(ownerIds, false, function(id) { return id.$oid; });
		$scope.shares.details = _.map(ownerIds, function(ownerId) { return $scope.users[ownerId.$oid]; });
		$scope.pushs.show = false;
		$scope.shares.show = true;
	}
	
	// select a criterion
	$scope.select = function(criterion) {
		// TODO
	}
	
	// close the given news group (either 'pushs' or 'shares')
	$scope.close = function(newsGroup) {
		$scope[newsGroup].show = false;
	}
	
	// mark all records as seen
	$scope.markAsSeen = function() {
		$http(jsRoutes.controllers.Users.clearPushed()).
			success(function() { $scope.pushed = []; }).
			error(function(err) { $scope.error = "Failed to mark pushed records as seen: " + err; });
		$http(jsRoutes.controllers.Users.clearShared()).
			success(function() { $scope.shared = []; }).
			error(function(err) { $scope.error = "Failed to mark shared records as seen: " + err; });
	}
	
	// hide news item
	$scope.hide = function(newsItemId) {
		$http(jsRoutes.controllers.News.hide(newsItemId.$oid)).
			success(function() {
				$scope.news.splice($scope.news.indexOf(newsItemId), 1);
				delete $scope.newsItems[newsItemId.$oid];
			}).
			error(function(err) { $scope.error = "Failed to hide this news item: " + err; });
	}
	
}]);