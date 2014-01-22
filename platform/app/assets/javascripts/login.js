var login = angular.module('login', []);
login.controller('LoginCtrl', ['$scope', '$http', function($scope, $http) {
	
	// init
	$scope.login = {};
	$scope.registration = {};
	
	// login
	$scope.login = function() {
		// check user input
		if (!$scope.login.email || !$scope.login.password) {
			$scope.login.error = "Please provide an email address and a password.";
			return;
		}
		
		// send the request
		var data = {"email": $scope.login.email, "password": $scope.login.password};
		$http.post(jsRoutes.controllers.Application.authenticate().url, JSON.stringify(data)).
			success(function(url) { window.location.replace(url); }).
			error(function(err) { $scope.login.error = err; });
	}
	
	// register new user
	$scope.register = function() {
		// check user input
		if (!$scope.registration.email || !$scope.registration.firstName || 
				!$scope.registration.lastName || !$scope.registration.password) {
			$scope.registration.error = "Please fill in all required fields.";
			return;
		}
		
		// send the request
		var data = {"email": $scope.registration.email, "firstName": $scope.registration.firstName,
				"lastName": $scope.registration.lastName, "password": $scope.registration.password};
		$http.post(jsRoutes.controllers.Application.register().url, JSON.stringify(data)).
			success(function(url) { window.location.replace(url); }).
			error(function(err) { $scope.registration.error = err; });
	}
	
}]);