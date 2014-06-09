var modules = {}; // will hold the handlers for modules defined in modules/js/handlers.js

var hdcSnpSnip = angular.module('hdcSnpSnip', [
  'ngRoute',
  'snpSnipControllers'
]);
 
hdcSnpSnip.config(['$routeProvider',
	function($routeProvider) {
    $routeProvider.
		when('/:records', {
			templateUrl: 'views/snpSnip.html',
			controller: 'SnpSnipCtrl'
		}).when('/', {
            templateUrl: 'views/snpSnip.html',
            controller: 'SnpSnipCtrl'
        });
	}
]);
