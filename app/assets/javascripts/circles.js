var circles = angular.module('circles', []);
circles.controller('CirclesCtrl', ['$scope', '$http', function($scope, $http) {
	
	// init
	$scope.error = null;
	$scope.newCircleName = null;
	$scope.circles = [];
	$scope.loadingContacts = false;
	$scope.contacts = [];
	$scope.foundUsers = [];
	$scope.searching = false;
	
	// fetch circles and make either given or first circle active
	$http(jsRoutes.controllers.Circles.fetch()).
		success(function(data) {
			$scope.circles = data;
			if ($scope.circles.length > 0) {
				var activeCircle = window.location.pathname.split("/")[2];
				if (activeCircle) {
					$scope.makeActive(_.find($scope.circles, function(circle) { return circle._id === activeCircle; }));
				} else {
					$scope.makeActive($scope.circles[0]);
				}
			}
		}).
		error(function(err) { $scope.error = "Failed to load circles: " + err; });
	
	// make circle tab active
	$scope.makeActive = function(circle) {
		_.each($scope.circles, function(circle) { circle.active = false; });
		circle.active = true;
	}
	
	// add a new circle
	$scope.addCircle = function() {
		$("#circleModal").modal("hide");
		var name = $scope.newCircleName;
		if (!name || name.length === 0) {
			$scope.error = "Please provide a name for the new circle.";
		} else {
			var data = {"name": name};
			$http.post(jsRoutes.controllers.Circles.add().url, data).
				success(function(newCircle) {
					$scope.error = null;
					$scope.newCircleName = null;
					$scope.circles.push(newCircle);
					$scope.makeActive(newCircle);
				}).
				error(function(err) { $scope.error = "Failed to add circle '" + name + "': " + err; });
		}
	}
	
	// delete a circle
	$scope.deleteCircle = function(circle) {
		$http(jsRoutes.controllers.Circles["delete"](circle._id)).
			success(function() {
				$scope.error = null;
				$scope.circles.splice($scope.circles.indexOf(circle), 1);
				if ($scope.circles.length > 0) {
					$scope.circles[0].active = true;
				}
			}).
			error(function(err) { $scope.error = "Failed to delete circle '" + circle.name + "': " + err; });
	}
	
	// load contacts
	$scope.loadContacts = function() {
		if ($scope.contacts.length === 0) {
			$scope.loadingContacts = true;
			$http(jsRoutes.controllers.Circles.loadContacts()).
				success(function(contacts) {
					$scope.error = null;
					$scope.contacts = contacts;
					$scope.loadingContacts = false;
				}).
				error(function(err) {
					$scope.error = "Failed to load contacts: " + err;
					$scope.loadingContacts = false;
				});
		}
	}
	
	// check whether user is not already in active circle
	$scope.isntMember = function(user) {
		var activeCircle = _.find($scope.circles, function(circle) { return circle.active; });
		return !_.contains(activeCircle.members, user._id);
	}
	
	// search for users
	$scope.searchUsers = function(circle) {
		$scope.searching = true;
		var query = circle.userQuery;
		if (query) {
		$http(jsRoutes.controllers.Users.search(query)).
			success(function(users) {
				$scope.error = null;
				$scope.foundUsers = users;
				$scope.searching = false;
			}).
			error(function(err) {
				$scope.error = "User search failed: " + err;
				$scope.searching = false;
			});
		}
	}
	
	// add a user
	$scope.addUsers = function(circle) {
		// get the users that should be added to the circle
		var contactsToAdd = _.filter($scope.contacts, function(contact) { return contact.checked; });
		var foundUsersToAdd = _.filter($scope.foundUsers, function(user) { return user.checked; });
		var usersToAdd = _.union(contactsToAdd, foundUsersToAdd);
		var userIds = _.map(usersToAdd, function(user) { return user._id; });
		userIds = _.uniq(userIds);
		
		var data = {"users": userIds};
		$http.post(jsRoutes.controllers.Circles.addUsers(circle._id).url, data).
			success(function() {
				$scope.error = null;
				$scope.foundUsers = [];
				_.each($scope.contacts, function(contact) { contact.checked = false; });
				_.each(userIds, function(userId) { circle.members.push(userId); });
			}).
			error(function(err) { $scope.error = "Failed to add users: " + err; });
	}
	
	// remove a user
	$scope.removeMember = function(circle, userId) {
		$http(jsRoutes.controllers.Circles.removeMember(circle._id, userId)).
			success(function() {
				$scope.error = null;
				circle.members.splice(circle.members.indexOf(userId), 1);
			}).
			error(function(err) { $scope.error = "Failed to remove the selected member from circle '" + circle.name + "': " + err; });
	}
}]);