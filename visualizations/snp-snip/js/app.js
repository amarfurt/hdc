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
