'use strict';

angular
  /**
   * Angular app for the Weight Watcher visualization.<br>
   * Here the route definition is done, only one route is configured:<br>
   * #/:records: This presents the main view which displays a BMI scale
   * with the latest BMI and weight of the user, plus some text details and
   * advices based on the latest information and the historical weight
   * data of the user.
   */
  .module('weightWatcherApp', [
    'ngRoute'
  ])
  .config(function ($routeProvider) {
    $routeProvider
      .when('/:records', {
        templateUrl: 'views/main.html',
        controller: 'MainCtrl'
      })
      .otherwise({
        redirectTo: '/'
      });
  });
