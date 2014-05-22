var http = require('http');
var fs = require('fs');
var url = require('url');
var dblite = require('dblite');
var querystring = require('querystring');

var resourceNotFound = function(response) {
response.writeHead(404, {"Content-Type": "text/plain"});
response.end("404 Not Found\n");
};

function onRequest(request, response) {
    var query = url.parse(request.url).query; 
    var rsNumber = querystring.parse(query).rs;
    var resource = querystring.parse(query).resource;
    
    response.setHeader("Access-Control-Allow-Origin", "*");
    
    var db = dblite('snp_snip.db');

    var data;

    if (resource === 'snpedia_text') {
        console.log("serving snpedia text");
        db.query(
            'SELECT html FROM snpedia WHERE rs = ?',
            [rsNumber],
            function (rows) {
                if (rows[0] && rows[0][0]) {
                    response.end(rows[0][0]);
                } else {
                    resourceNotFound(response);
                }
            }
        ); 
    } else if (resource === 'snpedia_strand_info') {
        console.log("serving snpedia strand info");
        db.query(
            'SELECT strand_info FROM snpedia WHERE rs = ?',
            [rsNumber],
            function (rows) {
                if (rows[0] && rows[0][0]) {
                    response.end(rows[0][0]);
                } else {
                    resourceNotFound(response);
                }
            }
        ); 
    } else if (resource === 'hapmap_chart_html') {
        console.log("serving hapmap chart html");
        db.query(
            'SELECT html FROM hapmap WHERE rs = ?',
            [rsNumber],
            function (rows) {
                if (rows[0] && rows[0][0]) {
                    response.end(rows[0][0]);
                } else {
                    resourceNotFound(response);
                }
            }
        ); 
    } else if (resource === 'hapmap_chart_image') {
        console.log("serving hapmap chart image");
        db.query(
            'SELECT image FROM hapmap WHERE rs = ?',
            [rsNumber],
            function (rows) {
                if (rows[0] && rows[0][0]) {
                    response.writeHead(200, {"Content-type" : "image/png"});
                    response.end(rows[0][0], 'binary');
                } else {
                    resourceNotFound(response);
                }
            }
        ); 
    } else {
        resourceNotFound(response);
        response.end();
    }

}

http.createServer(onRequest).listen(8888);
console.log("server has started.");

