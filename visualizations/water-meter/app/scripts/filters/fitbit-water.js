'use strict';

angular.module('waterMeterApp')
  /**
   * Filter and group the water records from fitbit.
   * The result is grouped by a date string written in YYYY-MM-DD format.
   */
  .filter('fitbitWater', function filterFitbitWaterRecords() {
    return function (input) {
      var filteredRecords =
        _.filter(input, function detectFitbitWaterRecords(element){
        return element.water !== undefined && element.water.length > 0;
      });
      return _.groupBy(filteredRecords, function groupByDate(element){
        return element.date;
      });
    };
  });
