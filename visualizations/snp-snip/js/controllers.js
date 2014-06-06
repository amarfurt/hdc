function changeOrientation(genotype) {
    var swap = {'A' : 'T', 'T' : 'A', 'C' : 'G', 'G' : 'C'};
    var types = genotype.split('');
    return swap[types[0]]+swap[types[1]];
}

function validRs(rs) {
    return rs.match(/^rs\d+$/);
}

function snpediaHandler($scope, $sce, snpediaData) {
    $scope.data[$scope.rs].snpediaText = $sce.trustAsHtml(snpediaData[0]);
    $scope.data[$scope.rs].snpediaOrientation = snpediaData[1];
    
}

function hapmapHandler($scope, $sce, hapmapData) {
    $scope.data[$scope.rs].hapmapChart = $sce.trustAsHtml(hapmapData[0]);
}

function dbsnpHandler($scope, $sce, dbsnpData) {
    $scope.data[$scope.rs].dbsnpGeneId = dbsnpData[0];
    $scope.data[$scope.rs].dbsnpSymbol = dbsnpData[1];
}

var dataHandlers = {
    'snpedia' : snpediaHandler,
    'hapmap' : hapmapHandler,
    'dbsnp' : dbsnpHandler
};

function getGenomeDataFromUrl($scope, $routeParams) {

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
    $scope.loadedSnpsCount = Object.keys($scope.snpMap).length; 
}

function prepareSearchResults($scope, $sce, rs) {

    $scope.invalidInput = !validRs(rs);

    if (validRs(rs)) {

        $scope.rs = rs;

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

        // get the required data if searching for this rs for the first time
        if (!$scope.data.hasOwnProperty(rs)) {
            $scope.data[rs] = {};

            var data = {};

            // get all data from the node server
            $.ajax({
                    url: "http://localhost:8888/?rs="+rs,
                    success: function(response) {
                        data = response;
                    },
                    async: false
            });

            // prepare the data received from the server
            for (resource in data) {
                if (dataHandlers.hasOwnProperty(resource)) {
                    dataHandlers[resource]($scope, $sce, data[resource]);
                    delete data[resource];
                }
            }

            // TODO
            // default for unhandled data

            // prepare personal genome data
            $scope.data[rs].userHas = $scope.snpMap.hasOwnProperty(rs);

            if ($scope.data[rs].userHas) {
                if ($scope.data[rs].snpediaOrientation === "minus") {
                    $scope.data[rs].orientation = "minus";
                    $scope.data[rs].genotype = changeOrientation($scope.snpMap[rs]);
                } else {
                    $scope.data[rs].orientation = "plus";
                    $scope.data[rs].genotype = $scope.snpMap[rs];
                }
            }
        }
    }
}

var controllers = angular.module('snpSnipControllers', ['ui.bootstrap']);
controllers.controller('SnpSnipCtrl', ['$scope', '$sce', '$routeParams', '$modal', '$log',
function($scope, $sce, $routeParams, $modal, $log) {

    $scope.loading = true;

    $scope.snpMap = {};
    $scope.searches = [];
    $scope.data = {};

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
