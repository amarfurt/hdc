moduleHandlers['hapmap'] = function($scope, $sce, hapmapData) {

    $scope.data[$scope.rs].hapmap = true;

    $scope.data[$scope.rs].hapmapChart = $sce.trustAsHtml(hapmapData[0]);

};
