var hdcRecordList = angular.module('hdcSnpInfo', [
  'ngRoute',
  'snpInfoControllers'
]);
 
hdcRecordList.config(['$routeProvider',
	function($routeProvider) {
    $routeProvider.
		when('/:records', {
			templateUrl: 'views/snpInfo.html',
			controller: 'SnpInfoCtrl'
		});
	}
]);
