/* To add a handler for database foo.db and template foo.html use the following skeleton:
 * moduleHandlers['foo'] = function($scope, fooData) {
 *
 *     // here process the tuple from the database contained in fooData as needed and add data to the scope for display in the view
 *
 * };
 */

moduleHandlers['dbsnp'] = function($scope, dbsnpData) {

    $scope.data[$scope.rs].dbsnp = true;

    $scope.data[$scope.rs].dbsnpGeneId = dbsnpData[0];
    $scope.data[$scope.rs].dbsnpSymbol = dbsnpData[1];

};

moduleHandlers['hapmap'] = function($scope, hapmapData) {

    $scope.data[$scope.rs].hapmap = true;

    $scope.data[$scope.rs].hapmapChart = $scope.angular['sce'].trustAsHtml(hapmapData[0]);

};

moduleHandlers['snpedia'] = function($scope, snpediaData) {

    $scope.data[$scope.rs].snpedia = true;

    $scope.data[$scope.rs].snpediaText = $scope.angular['sce'].trustAsHtml(snpediaData[0]);
    $scope.data[$scope.rs].snpediaOrientation = snpediaData[1];

};
