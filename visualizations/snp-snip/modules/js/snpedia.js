moduleHandlers['snpedia'] = function($scope, $sce, snpediaData) {

    $scope.data[$scope.rs].snpedia = true;

    $scope.data[$scope.rs].snpediaText = $sce.trustAsHtml(snpediaData[0]);
    $scope.data[$scope.rs].snpediaOrientation = snpediaData[1];

};
