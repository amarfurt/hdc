'use strict';

describe('Filter: workoutRecords', function () {

  // load the filter's module
  beforeEach(module('energyMeterApp'));

  // initialize a new instance of the filter before each test
  var workoutRecords;
  beforeEach(inject(function ($filter) {
    workoutRecords = $filter('workoutRecords');
  }));

  it('should return the input prefixed with "workoutRecords filter:"', function () {
    var text = 'angularjs';
    expect(workoutRecords(text)).toBe('workoutRecords filter: ' + text);
  });

});
