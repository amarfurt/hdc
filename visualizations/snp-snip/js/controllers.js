var changeOrientation = function(genotype) {
    swap = {'A' : 'T', 'T' : 'A', 'C' : 'G', 'G' : 'C'}
    types = genotype.split('');
    return swap[types[0]]+swap[types[1]];
}

var getGenomeDataFromUrl = function($scope, $routeParams) {

	// parse Base64 encoded uri and get the file
	if ($routeParams.records) {
		
        var record = "";
        $.ajax({
            url: atob($routeParams.records),
            success: function(data) {
                record = data;
            },
            async: false
        });

        // need to remove comments first
        record = record.replace(/\s*#.*$/gm, '');

        // extract the snp data 
        var snpData = $.csv.toArrays(record, {
            separator: '\t',
        });
        
        for (var x in snpData) {
            $scope.snpMap[snpData[x][0]] = snpData[x][3];
        }

	}

    // set scope variables for the loading message
    $scope.loaded_snps_count = Object.keys($scope.snpMap).length; 
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
        $scope.rs = null;
        $scope.genotype = null; 
        $scope.orientation = null;
        $scope.snpediaText = null;
        $scope.hapmapChart = null;
        $scope.geneId = null;
        $scope.symbol = null;

        $scope.rs = rs;
        $scope.userHas = $scope.snpMap.hasOwnProperty($scope.rs);

        if ($scope.userHas) {

            var strandInfo;
            $.ajax({
                url: "http://localhost:8888/?resource=snpedia_strand_info&rs="+$scope.rs,
                success: function(data) {
                    strandInfo = data;
                },
                async: false
            });

            if (strandInfo === "minus") {
                $scope.orientation = "minus";
                $scope.genotype = changeOrientation($scope.snpMap[$scope.rs]);
            } else {
                $scope.orientation = "plus";
                $scope.genotype = $scope.snpMap[$scope.rs];
            }
        }

        $.ajax({
            url: "http://localhost:8888/?resource=dbsnp_gene_id&rs="+$scope.rs,
            success: function(data) {
                $scope.geneId = data;
            },
            async: false
        });

        $.ajax({
            url: "http://localhost:8888/?resource=dbsnp_symbol&rs="+$scope.rs,
            success: function(data) {
                $scope.symbol = data;
            },
            async: false
        });

        $.ajax({
            url: "http://localhost:8888/?resource=snpedia_text&rs="+$scope.rs,
            success: function(data) {
                $scope.snpediaText = $sce.trustAsHtml(data);
            },
            async: false
        });
        
        $.ajax({
            url: "http://localhost:8888/?resource=hapmap_chart&rs="+$scope.rs,
            success: function(data) {
                $scope.hapmapChart = $sce.trustAsHtml(data);
            },
            async: false
        });
    }
};

var controllers = angular.module('snpSnipControllers', ['ui.bootstrap', 'compile']);
controllers.controller('SnpSnipCtrl', ['$scope', '$sce', '$routeParams', '$modal', '$log',
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
