'use strict';

angular.module('energyMeterApp')
  /**
   * Filter a list of raw records and retrieve only valid workout records.
   */
  .filter('workoutRecords', ['$filter', function ($filter) {
    return function (input) {
      var filteredRecords = _.filter(input,
        function detectWorkoutRecords(element){
          return element.meta &&
            element.meta.type.localeCompare('workoutList') === 0 &&
            parseInt(element.data.size) > 0;
        });
      var dateAdjustedrecords = _.map(filteredRecords,
        function adjustDate(element){
          element.meta.date = new Date(element.meta.date);
          return element;
        });
      var sortedRecords = _.sortBy(dateAdjustedrecords,
        function getDate(element){
          return element.meta.date;
        });
      var groupedRecords = _.groupBy(sortedRecords,
        function getWeekNumber(element){
          return $filter('date')(element.meta.date, 'yyyy-Www');
        });
      return groupedRecords;
    };
  }]);
