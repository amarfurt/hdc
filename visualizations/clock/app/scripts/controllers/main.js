'use strict';

angular.module('clockApp')
    /**
     * Main controller for the clock visualization, it implements the following
     * functions:
     * 1. Read the data from the URL parameters.
     * 2. Parse the data as JSON objects and filter/group it according
     *    to the expected format by the directive.
     * 3. Provide UI functionality to select the date of the data to display.
     * 4. Supply the selected data to the directive.
     */
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
            var filteredList = _.filter(recordList, function (element) {
                var type = element.meta.type;
                return type === 'moveList' || type === 'moveTicks' ||
                    type === 'sleepList' || type === 'sleepTicks';
            });

            var cleanRecordList = _.map(filteredList, function (element) {
                element.meta.date = new Date(element.meta.date);
                return element;
            });

            if (cleanRecordList.length === 0) {
                $scope.error = true;
                $scope.errorMessage =
                    'Sorry, we cannot find any move or sleep event';
            } else {
                // Sort and group records so that the lastest group comes first
                cleanRecordList = _.sortBy(cleanRecordList, function (element) {
                    return element.meta.date.getTime();
                }).reverse();

                var recordGroupList = [],
                    dateList = [],
                    currentDate = cleanRecordList[0].meta.date,
                    recordGroup = {
                        moveList: undefined,
                        moveTicks: undefined,
                        sleepList: undefined,
                        sleepTicks: undefined
                    };
                cleanRecordList.forEach(function (element) {
                    if (currentDate - element.meta.date === 0) {
                        recordGroup[element.meta.type] = element;
                    } else {
                        recordGroupList.push(recordGroup);
                        dateList.push(currentDate);

                        currentDate = element.meta.date;
                        recordGroup = {
                            moveList: undefined,
                            moveTicks: undefined,
                            sleepList: undefined,
                            sleepTicks: undefined
                        };
                        recordGroup[element.meta.type] = element;
                    }
                });

                dateList.push(currentDate);
                recordGroupList.push(recordGroup);

                $scope.recordGroupList = recordGroupList;
                $scope.selectedDateIndex = 0;
                $scope.dateList = dateList;
                $scope.moveList =
                    $scope.recordGroupList[$scope.selectedDateIndex].moveList;
                $scope.moveTicks =
                    $scope.recordGroupList[$scope.selectedDateIndex].moveTicks;
                $scope.sleepList =
                    $scope.recordGroupList[$scope.selectedDateIndex].sleepList;
                $scope.sleepTicks =
                    $scope.recordGroupList[$scope.selectedDateIndex].sleepTicks;

                $scope.$watch('selectedDateIndex', function (newValue) {
                    $scope.moveList =
                        $scope.recordGroupList[newValue].moveList;
                    $scope.moveTicks =
                        $scope.recordGroupList[newValue].moveTicks;
                    $scope.sleepList =
                        $scope.recordGroupList[newValue].sleepList;
                    $scope.sleepTicks =
                        $scope.recordGroupList[newValue].sleepTicks;
                });
            }
        }]);