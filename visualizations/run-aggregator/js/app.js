var hdcRunAggregator = angular.module('hdcRunAggregator', [
  'ngRoute',
  'runAggregatorCtrls'
]);
 
hdcRunAggregator.config(['$routeProvider',
  function($routeProvider) {
    $routeProvider.
      when('/:records', {
        templateUrl: 'views/runaggregator.html',
        controller: 'RunAggregatorCtrl'
      });
  }]);
