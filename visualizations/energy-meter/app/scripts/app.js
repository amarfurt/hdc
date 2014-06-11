'use strict';

angular
  /**
   * Define the app with a single route that is called when the records are
   * passed in the URL.
   */
  .module('energyMeterApp', [
    'ngRoute',
    'google-maps'
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
