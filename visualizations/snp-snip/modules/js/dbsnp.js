moduleHandlers['dbsnp'] = function($scope, $sce, dbsnpData) {

    $scope.data[$scope.rs].dbsnp = true;

    $scope.data[$scope.rs].dbsnpGeneId = dbsnpData[0];
    $scope.data[$scope.rs].dbsnpSymbol = dbsnpData[1];

};
