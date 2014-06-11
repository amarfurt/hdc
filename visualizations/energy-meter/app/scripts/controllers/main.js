'use strict';

angular.module('energyMeterApp')
  /**
   * Main controller for the visualization, it implements the following
   * functionality:
   * 1. Read the records from the URL parameters and pass them
   *    through the Jawbone workout filter.
   * 2. Implement the functionality for picking weeks using the HTML5 input
   *    element.
   * 3. Respond to changes in the selected week and display the weekly
   *    data in the map and the summary div.
   */
  .controller('MainCtrl', ['$scope', '$routeParams', '$filter',
    'JawboneWorkout',
    function ($scope, $routeParams, $filter, JawboneWorkout) {
      // Initialize basic variables for the display
      $scope.mapParams = {
        center : {
          latitude : 47.3667,
          longitude : 8.5500
        },
        zoom : 11,
      };

      $scope.weekDays = [
        {name : 'Sunday', colorClass : 'sunday'},
        {name : 'Monday', colorClass : 'monday'},
        {name : 'Tuesday', colorClass : 'tuesday'},
        {name : 'Wednesday', colorClass : 'wednesday'},
        {name : 'Thursday', colorClass : 'thursday'},
        {name : 'Friday', colorClass : 'friday'},
        {name : 'Saturday', colorClass : 'saturday'}
      ];

      $scope.error = {show : false};

      // Define functionality regarding the info window for each workout in
      // the map
      /**
       * Reset the info window object.
       */
      function resetInfoWindow(){
        $scope.infoWindow = {
          coords : {
            latitude : 0,
            longitude : 0,
          },
          show : false,
          workout : null,
        };
      }
      resetInfoWindow();
      /**
       * Show the info window when a marker is clicked. This calls $apply
       * because the click is triggered outside AngularJS.
       */
      $scope.showInfoWindow = function showInfoWindow($markerModel){
        $scope.infoWindow.show = true;
        $scope.infoWindow.coords.latitude = $markerModel.latitude;
        $scope.infoWindow.coords.longitude = $markerModel.longitude;
        $scope.infoWindow.workout = $markerModel.workout;
        $scope.$apply();
      };
      /**
       * Close the info window by resetting the info window object.
       * This calls $apply when called from a click in the map (i.e. outside an
       * AngularJS event).
       */
      $scope.closeInfoWindow = function closeInfoWindow(doApply){
        resetInfoWindow();
        if(doApply){
          $scope.$apply();
        }
      };

      // Functionality for legend collapse
      $scope.legendCollapsed = false;
      /**
       * Collapse/expand the legend when the title is clicked.
       */
      $scope.collapseLegend = function toggleLegend(){
        $scope.legendCollapsed = !$scope.legendCollapsed;
      };

      /**
       * Function that responds to changes in the selected week.
       * This checks if there is data for the given week and if then it
       * produces the marker models and summary object for display
       * in the view.
       */
      $scope.changedWeekSelection = function onWeekChange(){
        if($scope.dateForm.dateInput.$valid){
          $scope.closeInfoWindow(false);
          var weekNumber = $filter('date')($scope.selectedWeek, 'yyyy-Www');
          $scope.selectedWeekStart = new Date($scope.selectedWeek.getFullYear(),
            $scope.selectedWeek.getMonth(),
            $scope.selectedWeek.getDate() - $scope.selectedWeek.getDay());
          $scope.selectedWeekEnd = new Date($scope.selectedWeek.getFullYear(),
            $scope.selectedWeek.getMonth(),
            $scope.selectedWeek.getDate() - $scope.selectedWeek.getDay() + 6);
          if(_.has($scope.availableData, weekNumber)){
            $scope.dataPresent = true;
            $scope.weeklyMarkers = JawboneWorkout.generateMarkers(
              $scope.availableData[weekNumber]);
            $scope.summary = JawboneWorkout.generateSummary(
              $scope.availableData[weekNumber]);
            $scope.summary.ready = true;
            $scope.presentWeekDays = _.filter($scope.weekDays,
              function isDayPresent(day, idx){
                var found = _.find($scope.availableData[weekNumber],
                  function findDayInData(element){
                    return element.meta.date.getDay() === idx;
                  });
                return found !== undefined;
              });
          } else {
            $scope.dataPresent = false;
            $scope.weeklyMarkers = [];
            $scope.presentWeekDays = [];
            $scope.summary = {ready: false};
          }
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
      $scope.availableData = $filter('workoutRecords')(recordList);
      if(_.keys($scope.availableData).length === 0){
        $scope.error.show = true;
        $scope.error.message = 'Did not find any workout records in this space.';
      }
    }]);
