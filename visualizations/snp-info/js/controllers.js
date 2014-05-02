
var getGenomeDataFromUrl = function(scope, routeParams) {
	// parse Base64 encoded JSON records
	if (routeParams.records != null && scope.snpTable == null) {
		var records = JSON.parse(atob(routeParams.records));
		
		// need to remove comments (papaparse cant do it I think?)
		records[0] = records[0].replace(/\s*#.*$/gm, '');

		// extract the snp data 
		var snpData = $.parse(records[0], {
			delimiter: '\t',
			header: false,
		}).results;
		
		scope.snpMap = {};
		for (var x in snpData) {
			scope.snpMap[snpData[x][0]] = snpData[x][3];
		}
	}
}

var controllers = angular.module('snpInfoControllers', []);
controllers.controller('SnpInfoCtrl', ['$rootScope', '$scope', '$sce', '$location', '$routeParams',
function($rootScope, $scope, $sce, $location, $routeParams) {

	getGenomeDataFromUrl($rootScope, $routeParams);
	
	// helper to change path for search
	$scope.search = function(rs){
		$location.path('/'+$routeParams.records+'/'+rs);
	};

}]);

controllers.controller('SnpDetailCtrl', ['$scope', '$sce', '$routeParams',
function($scope, $sce, $routeParams) {

	getGenomeDataFromUrl($scope, $routeParams);
	
	// prepare search results
	$scope.rs = $routeParams.rs;
	$scope.userHas = $scope.snpMap.hasOwnProperty($scope.rs);
	if ($scope.userHas) {
		$scope.genotype = $scope.snpMap[$scope.rs];
	}

	if ($scope.snpediaText == null) {
		$.ajax({
			url: "http://localhost:8888/snpedia_text.html?rs="+$scope.rs,
			success: function(data) {
				$scope.snpediaText = $sce.trustAsHtml(data);
			},
			async: false
		});
	}
	
	$.ajax({
		url: "http://localhost:8888/hapmap_chart.png?rs="+$scope.rs,
		async: false
	});
	
	if ($scope.hapmapChart == null) {
		$.ajax({
			url: "http://localhost:8888/hapmap_chart.html?rs="+$scope.rs,
			success: function(data) {
				$scope.hapmapChart = $sce.trustAsHtml(data);
			},
			async: false
		});
	}

}]);