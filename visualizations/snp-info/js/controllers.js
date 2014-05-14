var getGenomeDataFromUrl = function($scope, $routeParams) {
	// parse Base64 encoded uri and get the file
	if ($routeParams.records != null && $.isEmptyObject($scope.snpTable)) {
		
        try {
            var record;
            $.ajax({
                url: atob($routeParams.records),
                success: function(data) {
                    record = data;
                },
                async: false
            });

            // need to remove comments (papaparse cant do it I think?)
            record = record.replace(/\s*#.*$/gm, '');

            // extract the snp data 
            var snpData = $.parse(record, {
                delimiter: '\t',
                header: false,
            }).results;
            
            for (var x in snpData) {
                $scope.snpMap[snpData[x][0]] = snpData[x][3];
            }
        } catch (err) {}
	}
};

var validRs = function (rs) {
    return rs.match(/^rs\d+$/);
}

var prepareSearchResults = function ($scope, $sce, rs) {

    $scope.invalidInput = !validRs(rs);
    if (validRs(rs)) {

        // tabs
        var index = $scope.searches.map(function(search){return search.rs}).indexOf(rs);
        if (index === -1) {
            $scope.searches.unshift({rs: rs, active: true});
        } else {
            for (i in $scope.searches) {
                $scope.searches[i].active = false;
            }
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
    controllers.controller('SnpInfoCtrl', ['$scope', '$sce', '$routeParams', '$modal', '$log',
    function($scope, $sce, $routeParams, $modal, $log) {

        $scope.loading = true;

        $scope.snpMap = {};
        $scope.searches = [];

        getGenomeDataFromUrl($scope, $routeParams);

        $scope.removeTab = function (index) {
            $scope.searches.splice(index, 1);
            if (index > 0) {
                $scope.searchUpdate($scope.searches[index - 1].rs);
            } else if (index < $scope.searches.length) {
                $scope.searchUpdate($scope.searches[index].rs);
            }
        };

        $scope.searchUpdate = function(rs) {
            prepareSearchResults($scope, $sce, rs);
        };

        $scope.open = function () {

            var modalInstance = $modal.open({
              templateUrl: 'views/help.html',
              size: 'lg',
            });

            modalInstance.result.then(function (selectedItem) {
                    $scope.selected = selectedItem;
                }, function () {
                    $log.info('Modal dismissed at: ' + new Date());
            });
        };

        $scope.loading = false;
}]);
