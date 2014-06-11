'use strict';

angular.module('weightWatcherApp')
  /**
   * Main controller for the weight watcher visualization.<br>
   * It executes the following actions:<br>
   *  1. Parse the data from the route parameters, filter it and sort it.<br>
   *  2. Define the data to be displayed in the weight scale directive and the
   *     details panel.<br>
   *  3. Retrieve the HDC tips from the message generator and display them.<br>
   *  4. Define the functionality for UI elements (e.g. pager for the tips).<br>
   */
  .controller('MainCtrl', ['$scope', '$routeParams', 'MessageGenerator',
    'BmiZones', function ($scope, $routeParams, MessageGenerator, BmiZones) {
      // Initialize a few scope variables.
      $scope.error = {show: false};

      // Retrieve the records from the URL object.
      var rawRecords = JSON.parse(atob($routeParams.records));
      var recordList = _.map(rawRecords, function(ele){
        try{
          return JSON.parse(ele);
        } catch(e){
          $scope.error.show = true;
          $scope.error.msg = 'Found a non-JSON record.' +
            'Please check the assigned records.';
        }
      });

      // Clean them by filtering the non-weight ones.
      var cleanRecordList = _.filter(recordList, function(ele){
        if(ele.weight && ele.weight.length > 0){
          return true;
        }
        return false;
      });
      if(cleanRecordList.length === 0){
        $scope.error.show = true;
        $scope.error.msg = 'Did not find any weight records in the space.';
        return;
      }

      // Sort them by date and reverse it to have the latest first.
      var sortedRecordList = _.sortBy(cleanRecordList, function(ele){
        var measurementDate = ele.weight[0].date;
        return new Date(measurementDate);
      }).reverse();

      // Define the latest measurement object to be used by the scale directive.
      $scope.latestMeasurement = sortedRecordList[0].weight[0];

      // Define the bmi zones to be displayed in the legend.
      var lowWeight = Math.floor($scope.latestMeasurement.weight - 10.0);
      var highWeight = Math.ceil($scope.latestMeasurement.weight + 10.0);
      var lowBmi = lowWeight * $scope.latestMeasurement.bmi /
        $scope.latestMeasurement.weight;
      var highBmi = highWeight * $scope.latestMeasurement.bmi /
        $scope.latestMeasurement.weight;
      $scope.bmiZones = BmiZones.getIntervals(lowBmi, highBmi);

      // Retrieve the messages from the message generator based on the sorted
      // data. Display the first message.
      $scope.messages = MessageGenerator.generateMessages(sortedRecordList);
      $scope.currentMsg = 0;

      // Define the handler functions for the pager UI element.
      $scope.previousDisabled = 'disabled';
      $scope.nextDisabled = $scope.messages.length > 1 ? '' : 'disabled';
      $scope.previousTip = function previousTip(){
        if($scope.currentMsg > 0){
          $scope.currentMsg--;
        }
      };
      $scope.nextTip = function nextTip(){
        if($scope.currentMsg < $scope.messages.length - 1){
          $scope.currentMsg++;
        }
      };
      $scope.$watch('currentMsg', function watchCurrentMsg(newValue){
        $scope.nextDisabled = '';
        $scope.previousDisabled = '';
        if(newValue === $scope.messages.length - 1){
          $scope.nextDisabled = 'disabled';
        }
        if(newValue === 0){
          $scope.previousDisabled = 'disabled';
        }
      });

      // Define the handlers for the UI collapser of the legend.
      $scope.legendCollapsed = false;
      $scope.collapseLegend = function collapseLegend(){
        $scope.legendCollapsed = !$scope.legendCollapsed;
      };
    }]);
