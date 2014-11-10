var fitbit = angular.module('fitbit', []);
fitbit.controller('ImportCtrl', ['$scope', '$http', '$location', 
	function($scope, $http, $location) {
		// init
		$scope.error = {};
		$scope.status = null;
		$scope.requesting = false;
		$scope.requested = 0;
		$scope.saving = false;
		$scope.saved = 0;
		$scope.measure = null;
		$scope.measurements = [
				{
					"name": "Water",
					"title": "Fitbit water consumption {date}",
					"endpoint": "/1/user/-/foods/log/water/date/{date}.json"
				},
				{
					"name": "Body Weight",
					"title": "Fitbit body weight {date}",
					"endpoint": "/1/user/-/body/log/weight/date/{date}.json"
				},
				{
					"name": "Sleep",
					"title": "Fitbit sleep {date}",
					"endpoint": "/1/user/-/sleep/minutesAwake/date/{date}/1d.json"
				}
		];
		var baseUrl = "https://api.fitbit.com";

		// init datepicker
		$("#datepicker").datepicker({
			"format": "M d, yyyy",
			"todayHighlight": true
		});

		// get authorization token
		var authToken = $location.path().split("/")[1];

		// start the importing of records
		$scope.startImport = function() {
			var fromDate = $("#fromDate").datepicker("getDate");
			var toDate = $("#toDate").datepicker("getDate");
			$scope.error.measure = !$scope.measure;
			$scope.error.date = (!isValidDate(fromDate) || !isValidDate(toDate));
			if ($scope.error.measure || $scope.error.date) {
				$scope.error.message = "Please fill in all fields.";
				return;
			} else if (fromDate > toDate) {
				$scope.error.date = true;
				$scope.error.message = "Start date must be before end date.";
				return;
			}
			$scope.error.message = null;
			importRecords(fromDate, toDate);
		}

		// checks whether the given date is valid
		isValidDate = function(date) {
			return date instanceof Date && isFinite(date);
		}
		
		// import records, one main record and possibly a detailed record for each day
		importRecords = function(fromDate, toDate) {
			$scope.error.messages = [];
			$scope.status = "Importing data from Fitbit...";
			$scope.requesting = true;
			$scope.requested = 0;
			$scope.saving = true;
			$scope.saved = 0;

			// import records explicitly for each day (we want to store it in that granularity)
			for (var curDate = fromDate; curDate <= toDate; curDate.setDate(curDate.getDate() + 1)) {
				// capture loop variable 'curDate'
				(function(date) {
					var formattedDate = date.getFullYear() + "-" + twoDigit(date.getMonth() + 1) + "-" + twoDigit(date.getDate());
					var data = {
						"authToken": authToken,
						"url": baseUrl + $scope.measure.endpoint.replace("{date}", formattedDate)
					};
					$scope.requested += 1;
					$http.post("https://" + window.location.hostname + ":9000/api/apps/oauth1", data).
						success(function(response) {
							// check if an error was returned
							if (response.errors) {
								errorMessage("Failed to import data on " + formattedDate + ": " + response.errors[0].message + ".");
							} else {
								saveRecord($scope.measure.title, formattedDate, response);
							}
						}).
						error(function(err) {
							errorMessage("Failed to import data on " + formattedDate + ": " + err);
						});
					})(curDate);
			}
			$scope.requesting = false;
		}

		// make a two digit string out of a given number
		twoDigit = function(num) {
			return ("0" + num).slice(-2);
		}

		// save a single record to the database
		saveRecord = function(title, formattedDate, record) {
			var name = title.replace("{date}", formattedDate);
			var data = {
					"authToken": authToken,
					"data": JSON.stringify(record),
					"name": name,
					"description": name
			};
			$http.post("https://" + window.location.hostname + ":9000/api/apps/create", data).
				success(function() {
					$scope.saved += 1;
					finish();
				}).
				error(function(err) {
					errorMessage("Failed to save record '" + name + "' to database: " + err);
				});
		}

		// handle errors during import
		errorMessage = function(errMsg) {
			$scope.error.messages.push(errMsg);
			finish();
		}

		// update application state at the end of an import
		finish = function() {
			if (!$scope.requesting && $scope.requested === $scope.saved + $scope.error.messages.length) {
				$scope.status = "Imported " + $scope.saved + " records.";
				if ($scope.error.messages.length > 0) {
					$scope.status = "Imported " + $scope.saved + " of " + $scope.requested + " records. For failures see error messages.";
				}
				$scope.saving = false;
			}
		}
	}
]);
