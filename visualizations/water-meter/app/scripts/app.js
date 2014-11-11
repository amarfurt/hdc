'use strict';

angular
  .module('waterMeterApp', [
    'ngRoute',
    'ngAnimate'
  ])
  /**
   * Configure the app with a single route for the path which reads the
   * records from the URL.
   */
  .config(function ($routeProvider) {
    $routeProvider
      .when('/:authToken', {
        templateUrl: 'views/main.html',
        controller: 'MainCtrl'
      })
      .otherwise({
        redirectTo: '/'
      });
  });
