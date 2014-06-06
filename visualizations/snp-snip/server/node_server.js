var http = require('http');
var fs = require('fs');
var url = require('url');
var dblite = require('dblite');
var querystring = require('querystring');

function onRequest(request, response) {
    // TODO just for testing, remove later
    response.setHeader("Access-Control-Allow-Origin", "*");

    var query = url.parse(request.url).query; 
    var rsNumber = querystring.parse(query).rs;
    
    var data = {}

    var files = fs.readdirSync();

    for (i in files) {
        m = file[i].match(/^(\w+)\.db$/);
        if (m) {
            var db = dblite(m[0]);
            var resource = m[1]; 
            db.query(
                'SELECT * FROM main WHERE rs = ?',
                [rsNumber],
                function (rows) {
                    if (rows) {
                        // splice to remove the rs column
                        data[resource] = rows[0].splice(0,1);
                    }
                }
            ); 
        }
    }

    response.contentType('application/json');
    response.end(JSON.stringify(data));
}
    
http.createServer(onRequest).listen(8888);
console.log("server has started. Listening on port 8888 ...");
