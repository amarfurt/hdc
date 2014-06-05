var http = require('http');
var fs = require('fs');
var url = require('url');
var dblite = require('dblite');
var querystring = require('querystring');

function resourceNotFound(response) {
    response.writeHead(404, {"Content-Type": "text/plain"});
    response.end("404 Not Found\n");
};

function hex2bin(hex)
{
    var bytes = [], str;

    for(var i=0; i< hex.length-1; i+=2)
        bytes.push(parseInt(hex.substr(i, 2), 16));

    return String.fromCharCode.apply(String, bytes);    
}

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
                    response.writeHead(200, {"Content-Type": "text/html"});
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
                    response.writeHead(200, {"Content-Type": "text/plain"});
                    response.end(rows[0][0]);
                } else {
                    resourceNotFound(response);
                }
            }
        ); 
    } else if (resource === 'hapmap_chart') {
        console.log("serving hapmap chart");
        db.query(
            'SELECT html FROM hapmap WHERE rs = ?',
            [rsNumber],
            function (rows) {
                if (rows[0] && rows[0][0]) {
                    response.writeHead(200, {"Content-Type": "text/html"});
                    response.end(rows[0][0]);
                } else {
                    resourceNotFound(response);
                }
            }
        ); 
    }  else if (resource === 'dbsnp_gene_id') {
        console.log("serving hapmap chart image");
        db.query(
            'SELECT gene_id FROM dbsnp WHERE rs = ?',
            [rsNumber],
            function (rows) {
                if (rows[0] && rows[0][0]) {
                    response.writeHead(200, {"Content-Type": "text/plain"});
                    response.end(rows[0][0]);
                } else {
                    resourceNotFound(response);
                }
            }
        ); 
    }  else if (resource === 'dbsnp_symbol') {
        console.log("serving hapmap chart image");
        db.query(
            'SELECT symbol FROM dbsnp WHERE rs = ?',
            [rsNumber],
            function (rows) {
                if (rows[0] && rows[0][0]) {
                    response.writeHead(200, {"Content-Type": "text/plain"});
                    response.end(rows[0][0]);
                } else {
                    resourceNotFound(response);
                }

            }
        ); 
    }  else {
        resourceNotFound(response);
        response.end();
    }
}

http.createServer(onRequest).listen(8888);
console.log("server has started. Listening on port 8888 ...");
