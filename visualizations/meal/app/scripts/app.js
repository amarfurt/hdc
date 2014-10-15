'use strict';

angular
    .module('mealApp', [
        'ngRoute',
        'ui.bootstrap'
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