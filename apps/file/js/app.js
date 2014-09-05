var hdcFileRecords = angular.module('hdcFileRecords', [
  'ngRoute',
  'fileRecordControllers'
]);
 
hdcFileRecords.config(['$routeProvider',
  function($routeProvider) {
    $routeProvider.
      when('/create/:replyTo', {
        templateUrl: 'views/create.html',
        controller: 'CreateCtrl'
      }).
      when('/details/:record', {
        templateUrl: 'views/details.html',
        controller: 'DetailsCtrl'
      });
  }]);
