'use strict';

angular.module('waterMeterApp')
  /**
   * Main controller for the visualization. It implements the following
   * functionality.
   * 1. Configure a date picker to select the day to display.
   * 2. Load an animation with the water glass directive based on the
   *    water consumption data for the selected day.
   */
  .controller('MainCtrl', ['$scope', '$interval', '$filter', '$routeParams',
    function ($scope, $interval, $filter, $routeParams) {
      // Initialize basic elements for the controller
      $scope.error = {show : false};
      var recommendedWaterConsumption = 2000;
      var fillingGlassesPromise = null;
      $scope.waterGlasses = [[], []];

      /**
       * React to date changes by modifying the message displayed in the main
       * jumbotron in the HTML template and initiate the animation with the
       * glasses filled up depending on the day water value.
       */
      $scope.dateChanged = function dateChanged(){
        $scope.noDataInDate = false;
        $scope.goalAccomplished = false;
        $scope.goalUnaccomplished = false;
        if($scope.dateForm.inputDate.$valid){
          var currentWaterLevel = 0;
          if(_.has($scope.waterData, $scope.selectedDate)){
            var dayWater = $scope.waterData[$scope.selectedDate];
            if(dayWater.summary.water >= recommendedWaterConsumption){
              $scope.goalAccomplished = true;
            } else {
              $scope.goalUnaccomplished = true;
            }
            currentWaterLevel = dayWater.summary.water;
          } else {
            $scope.noDataInDate = true;
          }
          $scope.waterGlasses = [[], []];
          if(fillingGlassesPromise !== null){
            $interval.cancel(fillingGlassesPromise);
          }
          fillingGlassesPromise = $interval(function fillGlasses(){
            $scope.waterGlasses[$scope.waterGlasses[0].length === 5 ? 1 : 0]
              .push({
                value: currentWaterLevel > 200 ? 200 : currentWaterLevel
              });
            currentWaterLevel -= currentWaterLevel > 200 ? 200 :
              currentWaterLevel;
          }, 100, 10);
        }
      };

      // Standard retrieval of records from URL and JSON parsing.
      var rawRecords = JSON.parse(atob($routeParams.records));
      var recordList = _.map(rawRecords, function(ele){
        try{
          return JSON.parse(ele);
        } catch(e){
          $scope.error.show = true;
          $scope.error.message = 'Found a non-JSON record. ' +
            'Please check the assigned records.';
        }
      });
      $scope.filteredData = $filter('fitbitWater')(recordList);
      if(_.keys($scope.filteredData).length === 0){
        $scope.error.show = true;
        $scope.error.message = 'Did not find any water records in this space.';
      }
      $scope.waterData = {};
      _.each($scope.filteredData, function pluck(element, key){
        $scope.waterData[key] = element[0];
      });
    }]);
