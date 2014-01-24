var messages = angular.module('messages', []);
messages.controller('MessagesCtrl', ['$scope', '$http', function($scope, $http) {
	
	// init
	$scope.error = null;
	$scope.loading = true;
	$scope.messages = [];
	$scope.names = {};
	
	// get current user
	$http(jsRoutes.controllers.Users.getCurrentUser()).
		success(function(userId) { getMessages(userId); });
	
	// get messages
	getMessages = function(userId) {
		var properties = {"receiver": userId};
		var fields = ["_id", "sender", "created", "title"];
		var data = {"properties": properties, "fields": fields};
		$http.post(jsRoutes.controllers.Messages.get().url, JSON.stringify(data)).
			success(function(messages) {
				$scope.messages = messages;
				getSenderNames();
			}).
			error(function(err) {
				$scope.error = "Failed to load message: " + err;
				$scope.loading = false;
			});
	}
	
	getSenderNames = function() {
		var senderIdStrings = _.uniq(_.map($scope.messages, function(message) { return message.sender.$oid; }));
		var senderIds = _.map(senderIdStrings, function(senderIdString) { return {"$oid": senderIdString}; });
		var data = {"properties": {"_id": senderIds}, "fields": ["name"]};
		$http.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data)).
			success(function(users) {
				_.each(users, function(user) { $scope.names[user._id.$oid] = user.name; });
				$scope.loading = false;
			}).
			error(function(err) {
				$scope.error = "Failed to load user names: " + err;
				$scope.loading = false;
			});
	}
	
	// open message details
	$scope.showMessage = function(message) {
		window.location.href = jsRoutes.controllers.Messages.details(message._id.$oid).url;
	}
	
}]);
messages.controller('CreateMessageCtrl', ['$scope', '$http', function($scope, $http) {
	
	// init
	$scope.error = null;
	$scope.success = null;
	$scope.message = {};
	
	// send the message
	$scope.sendMessage = function() {
		$scope.success = null;
		// check input
		if (!$scope.message.receiver) {
			$scope.error = "Please enter a receiver for your message.";
			return;
		} else if (!$scope.message.title) {
			$scope.error = "Please enter a subject for your message.";
			return;
		} else {
			$scope.error = null;
		}
		
		// send request
		var data = {"receiver": $scope.message.receiver, "title": $scope.message.title, "content": $scope.message.content};
		$http.post(jsRoutes.controllers.Messages.send().url, JSON.stringify(data)).
			success(function() {
				$scope.success = "Your message was sent.";
				$scope.message = {};
			}).
			error(function(err) { $scope.error = err; });
	}
	
}]);