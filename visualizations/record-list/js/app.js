var hdcRecordList = angular.module('hdcRecordList', [
  'ngRoute',
  'recordListControllers'
]);
 
hdcRecordList.config(['$routeProvider',
  function($routeProvider) {
    $routeProvider.
      when('/:cacheId', {
        templateUrl: 'views/recordlist.html',
        controller: 'RecordListCtrl'
      });
  }]);
