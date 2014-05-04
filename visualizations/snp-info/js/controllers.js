
var getGenomeDataFromUrl = function($scope, $routeParams) {
	// parse Base64 encoded JSON records
	if ($routeParams.records != null && $scope.snpTable == null) {
		var records = JSON.parse(atob($routeParams.records));
		
		// need to remove comments (papaparse cant do it I think?)
		records[0] = records[0].replace(/\s*#.*$/gm, '');

		// extract the snp data 
		var snpData = $.parse(records[0], {
			delimiter: '\t',
			header: false,
		}).results;
		
		$scope.snpMap = {};
		for (var x in snpData) {
			$scope.snpMap[snpData[x][0]] = snpData[x][3];
		}
	}
};

var prepareSearchResults = function ($scope, $sce, rs) {

    if (rs) {
        $scope.searched = true;
    } else {
        $scope.searched = false;
    }

    $scope.rs = rs;
    $scope.userHas = $scope.snpMap.hasOwnProperty($scope.rs);
    if ($scope.userHas) {
        $scope.genotype = $scope.snpMap[$scope.rs];
    }

    $.ajax({
        url: "http://localhost:8888/"+$scope.rs+"/snpedia_text.html",
        success: function(data) {
            $scope.snpediaText = $sce.trustAsHtml(data);
        },
        async: false
    });
    
    $scope.imageSource = "http://localhost:8888/"+$scope.rs+"/hapmap_chart.png";
    
    $.ajax({
        url: "http://localhost:8888/"+$scope.rs+"/hapmap_chart.html",
        success: function(data) {
            $scope.hapmapChart = $sce.trustAsHtml(data);
        },
        async: false
    });

};

var controllers = angular.module('snpInfoControllers', []);
controllers.controller('SnpInfoCtrl', ['$scope', '$sce', '$routeParams',
function($scope, $sce, $routeParams) {

	getGenomeDataFromUrl($scope, $routeParams);
	
    $scope.searchUpdate = function(rs) {
        prepareSearchResults($scope, $sce, rs);
    };
}]);
