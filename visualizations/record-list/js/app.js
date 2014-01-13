var hdcRecordList = angular.module('hdcRecordList', [
  'ngRoute',
  'recordListControllers'
]);
 
hdcRecordList.config(['$routeProvider',
  function($routeProvider) {
    $routeProvider.
      when('/:records', {
        templateUrl: 'views/recordlist.html',
        controller: 'RecordListCtrl'
      });
  }]);
