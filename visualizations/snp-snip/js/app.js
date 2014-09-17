var modules = {}; // will hold the handlers for modules defined in modules/js/handlers.js

var hdcSnpSnip = angular.module('hdcSnpSnip', []);
hdcSnpSnip.controller('SnpSnipCtrl', ['$scope', '$http', '$sce', '$location',
	function($scope, $http, $sce, $location) {

		// export sce to module handlers
		$scope.angular = {'sce': $sce};

		$scope.searches = [];
		$scope.data = {};
		$scope.records = {};
		$scope.selectedRecord = null;
		$scope.input = null;

		// parse the authorization token from the url
		var authToken = $location.path().split("/")[1];

		// get record ids
		var data = {"authToken": authToken};
		$http.post("https://" + window.location.hostname + ":9000/api/visualizations/ids", JSON.stringify(data)).
			success(function(recordIds) {
				// load the full records at the beginning
				// replace this by lazy loading once JSON format for records is implemented
				getGenomeData(recordIds);
			}).
			error(function(err) {
				$scope.importFailed = true;
			});

		$scope.makeActive = function(search) {
			$scope.rs = search.rs;
			for (i in $scope.searches) {
				$scope.searches[i].active = false;
			}
			search.active = true;
		}

		$scope.removeTab = function (search) {
			var index = $scope.searches.map(function(s){return s.rs}).indexOf(search.rs);
			if (index >= 0) {
				$scope.searches.splice(index, 1);
				if ($scope.searches.length && search.active) {
					var newActive = (index === 0) ? 0 : index - 1;
					$scope.makeActive($scope.searches[newActive]);
				}
			}
		};

		$scope.searchUpdate = function() {
			prepareSearchResults($scope.input);
		};

		$scope.changeOrientation = function(genotype) {
			var swap = {'A' : 'T', 'T' : 'A', 'C' : 'G', 'G' : 'C'};
			var types = genotype.split('');
			return swap[types[0]]+swap[types[1]];
		}

		$scope.isValidRs = function(rs) {
			return rs.match(/^rs\d+$/);
		}

		$scope.recordCount = function() {
			return Object.keys($scope.records).length;
		}

		getGenomeData = function(recordIds) {
			var data = {"authToken": authToken};
			data.properties = {"_id": recordIds};
			data.fields = ["name", "data"];
			$http.post("https://" + window.location.hostname + ":9000/api/visualizations/records", JSON.stringify(data)).
				success(function(records) {
					for (var i = 0; i < records.length; i++) {
						// parse record
						try {
							var data = JSON.parse(records[i].data);
						} catch(parsingError) {
							// skip this record
							continue;
						}

						// save retrieved data
						var curId = records[i]._id.$oid;
						if (!$scope.records[curId]) {
							$scope.records[curId] = data;
							$scope.records[curId].name = records[i].name;
							$scope.records[curId].id = curId;
						} else {
							for (num in data) {
								$scope.records[curId][num] = data[num];
							}
						}
					}
					if (records.length) {
						$scope.selectedRecord = $scope.records[records[0]._id.$oid];
					}
					$scope.imported = true;
				}).
				error(function(err) {
					$scope.importFailed = true;
				});
		}

		prepareSearchResults = function(rs) {

			$scope.invalidInput = !$scope.isValidRs(rs);
			$scope.loadingDataFailed = false;

			if ($scope.isValidRs(rs)) {

				$scope.rs = rs;

				// tabs
				var index = $scope.searches.map(function(search){return search.rs}).indexOf(rs);
				if (index === -1) {
					$scope.searches.push({rs: rs, active: true});
					index = $scope.searches.length - 1;
				}
				$scope.makeActive($scope.searches[index]);

				// get the required data if searching for this rs for the first time
				if (!$scope.data.hasOwnProperty(rs)) {
					$scope.data[rs] = {};

					var response;

					// get all data from the node server
					$http.get("https://" + window.location.hostname + ":5000/snp-snip/?rs="+rs).
						success(function(response) {

								$scope.data[rs].resources = Object.keys(response).sort(function(r1, r2){
									if (!(modules[r1] && modules[r2])) {
										return 0;
									} else {
										return (modules[r1].position < modules[r2].position) ? -1 : 1;
									}
								});
								var resources = Object.keys(response).sort(function(r1, r2){
									if (!(modules[r1] && modules[r2])) {
										return 0;
									} else {
										return (modules[r1].priority < modules[r2].priority) ? 1 : -1;
									}
								});

								// prepare the data received from the server
								for (i in resources) {
									if (modules.hasOwnProperty(resources[i])) {
										modules[resources[i]].handler($scope, response[resources[i]]);
									}
								}

								// prepare personal genome data
								$scope.data[rs].genotypes = {};
								if ($scope.data[rs].snpediaOrientation === "minus") {
									$scope.data[rs].orientation = "minus";
								} else {
									$scope.data[rs].orientation = "plus";
								}
								for (i in $scope.records) {
									if ($scope.records[i][rs]) {
										if ($scope.data[rs].snpediaOrientation === "minus") {
											$scope.data[rs].genotypes[i] = $scope.changeOrientation($scope.records[i][rs].genotype);
										} else {
											$scope.data[rs].genotypes[i] = $scope.records[i][rs].genotype;
										}
									}
								}
						}).
						error(function(err) {
							$scope.loadingDataFailed = true;
						});
				}
			}
		}
	}
]);
