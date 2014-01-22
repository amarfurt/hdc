var messages = angular.module('messages', []);
messages.controller('MessagesCtrl', ['$scope', '$http', function($scope, $http) {
	
	// init
	$scope.error = null;
	$scope.messages = [];
	
	// get current user
	$http(jsRoutes.controllers.Users.getCurrentUser()).
		success(function(userId) { getMessages(userId); });
	
	// get messages
	getMessages = function(userId) {
		var properties = {"receiver": userId};
		var fields = ["_id", "sender", "title"];
		var data = {"properties": properties, "fields": fields};
		$http.post(jsRoutes.controllers.Messages.get().url, JSON.stringify(data)).
			success(function(data) { $scope.messages = data; }).
			error(function(err) { $scope.error = "Failed to load message: " + err; });
	}
	
	// open message details
	$scope.showMessage = function(message) {
		window.location.href = jsRoutes.controllers.Messages.details(message._id).url;
	}
	
}]);