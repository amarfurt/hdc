'use strict';

describe('Directive: waterGlass', function () {

  // load the directive's module
  beforeEach(module('waterMeterApp'));

  var element,
    scope;

  beforeEach(inject(function ($rootScope) {
    scope = $rootScope.$new();
  }));

  it('should make hidden element visible', inject(function ($compile) {
    element = angular.element('<water-glass></water-glass>');
    element = $compile(element)(scope);
    expect(element.text()).toBe('this is the waterGlass directive');
  }));
});
