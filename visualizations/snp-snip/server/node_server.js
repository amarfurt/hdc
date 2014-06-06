var http = require('http');
var fs = require('fs');
var url = require('url');
var dblite = require('dblite');
var querystring = require('querystring');
var async = require('async');

function onRequest(request, response) {
    // TODO cors just for testing, remove later
    response.setHeader("Access-Control-Allow-Origin", "*");
    response.setHeader('content-type', 'application/json');

    var query = url.parse(request.url).query; 
    var rsNumber = querystring.parse(query).rs;
    
    var files = fs.readdirSync('.').filter(function(filename){return /^\w+\.db$/.test(filename)});
    var tasks = {};

    for (i in files) {

        // oh god why
        var resource = files[i].match(/^(\w+)\.db$/)[1];
        tasks[resource] = function(filename) { 
            return function(callback) {
                dblite(filename).query(
                    'SELECT * FROM main WHERE rs = ?',
                    [rsNumber],
                    function(rows) {
                        if (rows && rows[0]) {

                            // remove the rs column
                            rows[0].shift();

                            callback(null, rows[0]); 
                        } else {
                            callback(null, null);
                        }
                    }
                );
            };
        }(files[i]);

    }

    async.parallel(tasks, function(err, results) {
        for {resource in results) {
            if (results[resource] == null) {
                delete results[resource];
            }
        }
        console.log(JSON.stringify(results));
        response.end(JSON.stringify(results));
    });
}
    
http.createServer(onRequest).listen(8888);
console.log("server has started. Listening on port 8888 ...");
