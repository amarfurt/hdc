var hdcRecordList = angular.module('hdcSnpInfo', [
  'ngRoute',
  'snpInfoControllers'
]);
 
hdcRecordList.config(['$routeProvider',
	function($routeProvider) {
    $routeProvider.
		when('/:records', {
			templateUrl: 'views/main.html',
			controller: 'SnpInfoCtrl'
		}).when('/:records/:rs', {
			templateUrl: 'views/detail.html',
			controller: 'SnpDetailCtrl'
		});
	}
]);
