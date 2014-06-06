function changeOrientation(genotype) {
    var swap = {'A' : 'T', 'T' : 'A', 'C' : 'G', 'G' : 'C'};
    var types = genotype.split('');
    return swap[types[0]]+swap[types[1]];
}

function validRs(rs) {
    return rs.match(/^rs\d+$/);
}

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

var moduleHandlers = {};

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

            var response;

            // get all data from the node server
            $.ajax({
                    url: "http://localhost:8888/?rs="+rs,
                    success: function(data) {
                        response = data;
                    },
                    async: false
            });

            $scope.data[rs].resources = Object.keys(response);

            // prepare the data received from the server
            for (resource in response) {
                if (moduleHandlers.hasOwnProperty(resource)) {
                    moduleHandlers[resource]($scope, response[resource]);
                }
            }

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

    // convenient way access them in module handlers
    $scope.angular = {'sce': $sce, 'routeParams': $routeParams, 'modal': $modal, 'log': $log};

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

    $scope.help = function () {

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
