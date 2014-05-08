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

var validRs = function (rs) {
    return rs.match(/rs\d+/);
}

var prepareSearchResults = function ($scope, $sce, rs) {

    $scope.invalidInput = !validRs(rs);

    if (validRs(rs)) {

        $scope.searched = true;

        // tabs
        var index = $scope.searches.map(function(search){return search.rs}).indexOf(rs);
        if (index === -1) {
            $scope.searches.unshift({rs: rs, active: true});
        } else {
            $scope.searches[index].active = true;
        }

        // search results
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
            error: function() {
                $scope.snpediaText = null;
            },
            async: false
        });
        
        
        $.ajax({
            url: "http://localhost:8888/"+$scope.rs+"/hapmap_chart.html",
            success: function(data) {
                $scope.hapmapChart = data;
                $scope.hapmapImageSource = "http://localhost:8888/"+$scope.rs+"/hapmap_chart.png";
            },
            error: function() {
                $scope.hapmapChart = null;
                $scope.imageSource = null;
            },
            async: false
        });

        $scope.firstAccordion = true;
        $scope.secondAccordion = true;
        $scope.thirdAccordion = true;
        }
    };

    var controllers = angular.module('snpInfoControllers', ['ui.bootstrap', 'compile']);
    controllers.controller('SnpInfoCtrl', ['$scope', '$sce', '$routeParams',
    function($scope, $sce, $routeParams) {

        getGenomeDataFromUrl($scope, $routeParams);
        
        $scope.searches = [];

        $scope.searchUpdate = function(rs) {
            prepareSearchResults($scope, $sce, rs);
        };

}]);
