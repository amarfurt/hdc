'use strict';

angular.module('clockApp', ['ngRoute', 'ui.bootstrap'])
    /**
     * Configure a single route to the app when records are passed through the
     * URL.
     */
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