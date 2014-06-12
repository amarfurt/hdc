'use strict';

angular.module('mealApp')
  .controller('MainCtrl', ['$scope', '$routeParams',
    function ($scope, $routeParams) {
        $scope.error = false;
        $scope.errorMessage = '';

        var rawRecords = JSON.parse(atob($routeParams.records));
        var recordList = _.map(rawRecords, function(ele){
            try{
                return JSON.parse(ele);
            } catch(e){
                $scope.error = true;
                $scope.errorMessage = 'Found a non-JSON record. ' +
                    'Please check the assigned records.';
            }
        });
        var cleanRecordList = _.filter(recordList, function (element) {
            var type = element.meta.type;
            return type === 'mealList' && element.data.size > 0;
        });

        if (cleanRecordList.length === 0) {
            $scope.error = true;
            $scope.errorMessage = 'Sorry, we cannot find any meal record';
        } else {
            cleanRecordList = _.map(cleanRecordList, function (element) {
                element.meta.date = new Date(element.meta.date);
                return element;
            });
            cleanRecordList = _.sortBy(cleanRecordList, function (element) {
                return element.meta.date.getTime();
            }).reverse();

            var dateList = [];

            cleanRecordList.forEach(function (element) {
                dateList.push(element.meta.date);
            });

            $scope.recordList = cleanRecordList;
            $scope.selectedDateIndex = 0;
            $scope.dateList = dateList;
            $scope.mealList = $scope.recordList[$scope.selectedDateIndex];

            $scope.$watch('selectedDateIndex', function (newValue) {
                $scope.mealList = $scope.recordList[newValue];
            });
        }
    }]);
