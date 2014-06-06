moduleHandlers['dbsnp'] = function($scope, $sce, dbsnpData) {

    $scope.data[$scope.rs].dbsnp = true;

    $scope.data[$scope.rs].dbsnpGeneId = dbsnpData[0];
    $scope.data[$scope.rs].dbsnpSymbol = dbsnpData[1];

};

moduleHandlers['hapmap'] = function($scope, $sce, hapmapData) {

    $scope.data[$scope.rs].hapmap = true;

    $scope.data[$scope.rs].hapmapChart = $sce.trustAsHtml(hapmapData[0]);

};

moduleHandlers['snpedia'] = function($scope, $sce, snpediaData) {

    $scope.data[$scope.rs].snpedia = true;

    $scope.data[$scope.rs].snpediaText = $sce.trustAsHtml(snpediaData[0]);
    $scope.data[$scope.rs].snpediaOrientation = snpediaData[1];

};
