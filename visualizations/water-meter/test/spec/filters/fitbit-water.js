'use strict';

describe('Filter: fitbitWater', function () {

  // load the filter's module
  beforeEach(module('waterMeterApp'));

  // initialize a new instance of the filter before each test
  var fitbitWater;
  beforeEach(inject(function ($filter) {
    fitbitWater = $filter('fitbitWater');
  }));

  it('should return the input prefixed with "fitbitWater filter:"', function () {
    var text = 'angularjs';
    expect(fitbitWater(text)).toBe('fitbitWater filter: ' + text);
  });

});
