'use strict';

angular.module('timeSeriesApp')
  /**
   * Main controller for the time series visualization, it takes care of
   * the following functionality.
   * 1. Read the data from the URL params and clear it
   * 2. Provide the available datasets in a control that allows the user
   *    control over what is displayed.
   * 3. Attach the selected datasets to the time-series directive for display.
   * 4. Allow the removal of displayed datasets.
   * 5. Ensure that no more than 5 datasets are showed in the directive
   *    at the same time.
   */
  .controller('MainCtrl', ['$scope', '$routeParams', 'FitbitResources',
    function ($scope, $routeParams, FitbitResources) {
      // Initialize the error variable
      $scope.error = {show: false};

      // Read the raw data from the url.
      var rawRecords = JSON.parse(atob($routeParams.records));

      // Parse all the objects and ensure they are Javascript Objects.
      var recordList = _.map(rawRecords, function(ele){
        try{
          return JSON.parse(ele);
        } catch(e){
          $scope.error.show = true;
          $scope.error.msg = 'Found a non-JSON record. ' +
            'Please check the assigned records.';
        }
      });

      // Clean the list of records according to the defined resources
      // in the fitbit service
      var cleanList = _.filter(recordList, function(ele){
        return _.keys(ele).length === 1 &&
          _.find(FitbitResources.timeSeriesResourcesKeys, function findKey(val){
            return val.localeCompare(_.keys(ele)[0]) === 0;
          }) !== undefined;
      });
      if(cleanList.length === 0){
        $scope.error.show = true;
        $scope.error.msg = 'Did not find any valid records for the space.';
        return;
      }

      // Manipulate the records to adapt them to the directive's data format
      // this implies creating Javascript Date objects and ensuring
      // that the values are float
      $scope.availableDataSets = _.map(cleanList, function transformRecord(ele){
        var dataKey = _.keys(ele)[0];
        var modifiedElement = {
          title: FitbitResources.timeSeriesResources[dataKey].title,
          units : FitbitResources.timeSeriesResources[dataKey].units,
          data : _.map(ele[dataKey], function transformData(subEle){
            return {
              datetime: new Date(subEle.dateTime),
              value : parseFloat(subEle.value)
            };
          })
        };
        return modifiedElement;
      });
      $scope.displayedDatasets = [];
      $scope.displayTimeSeries = false;

      /**
       * UI control that determines when the display button can be clicked.
       * This is done to control issues with datasets already added
       * or when the number of displayed datasets is over 5.
       */
      function disableDisplayButton(){
        if($scope.selectedDataset){
          var found = _.find($scope.displayedDatasets, function(ele){
            return ele.title.localeCompare($scope.selectedDataset.title) === 0;
          });
          if(found !== undefined){
            $scope.disableDisplayButton = true;
          } else if($scope.displayedDatasets.length < 5){
            $scope.disableDisplayButton = false;
          } else if($scope.displayedDatasets.length > 5){
            $scope.disableDisplayButton = true;
          }
        } else {
          $scope.disableDisplayButton = true;
        }
      }
      $scope.$watch('selectedDataset', disableDisplayButton);

      /**
       * Display the selected dataset in the graph.
       */
      $scope.displayDataset = function displayDataset(){
        if($scope.selectedDataset && $scope.displayedDatasets.length < 5){
          $scope.displayedDatasets.push($scope.selectedDataset);
          disableDisplayButton();
          $scope.displayTimeSeries = true;
        }
      };

      /**
       * Remove a dataset from the graph.
       */
      $scope.hideDataset = function hideDataset(datasetIndex){
        $scope.displayedDatasets.splice(datasetIndex, 1);
        disableDisplayButton();
        if($scope.displayedDatasets.length === 0){
          $scope.displayTimeSeries = false;
        }
      };
    }]);
