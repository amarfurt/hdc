var messages = angular.module('messages', []);
messages.controller('MessagesCtrl', ['$scope', '$http', function($scope, $http) {
	
	// init
	$scope.error = null;
	$scope.messages = [];
	
	// fetch messages
	$http(jsRoutes.controllers.Messages.fetch()).
		success(function(data) { $scope.messages = data; }).
		error(function(err) { $scope.error = "Failed to load message: " + err; });
	
	// open message details
	$scope.showMessage = function(message) {
		window.location.href = jsRoutes.controllers.Messages.details(message._id).url;
	}
	
}]);