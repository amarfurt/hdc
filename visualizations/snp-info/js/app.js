var hdcSnpInfo = angular.module('hdcSnpInfo', [
  'ngRoute',
  'snpInfoControllers'
]);
 
hdcSnpInfo.config(['$routeProvider',
	function($routeProvider) {
    $routeProvider.
		when('/:records', {
			templateUrl: 'views/snpInfo.html',
			controller: 'SnpInfoCtrl'
		}).when('/', {
            templateUrl: 'views/snpInfo.html',
            controller: 'SnpInfoCtrl'
        });
	}
]);
