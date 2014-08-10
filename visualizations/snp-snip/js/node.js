var http = require('http');
var fs = require('fs');
var url = require('url');
var dblite = require('dblite');
var querystring = require('querystring');
var async = require('async');
var csv = require('fast-csv');

var process = function(request, response) {

    // TODO cors just for testing, remove later
    response.setHeader("Access-Control-Allow-Origin", "*");

    response.setHeader('content-type', 'application/json');

    var query = url.parse(request.url).query; 

    if (querystring.parse(query).id) {
        // handle request for retrieving and parsing the 23andme file

        var cacheId = querystring.parse(query).id;

        http.get("http://localhost:9001/cache/" + cacheId, function(resp){
            var json = "";
            resp.on("data", function(chunk){
                json += chunk;
            }).on('end', function(){

                var records = {};
                try { 
                    records = JSON.parse(json);
                } catch (exception) {
                }

                var tasks = {};
                for (i in records) {
                    tasks[JSON.parse(records[i].data).title] = function(record) {

                        return function(callback) {

                            var name;
                            var content;

                            try {
                                name = JSON.parse(record.data).title;
                                content = JSON.parse(record.data).data;
                            } catch (exception) {
                            }

                            if (!content) {
                                content = '';
                            }

                            // extract and remove the comment at the top 
                            m = content.match(/^\s*#.*$/gm);
                            var comment;
                            if (m) {
                                comment = m.join('');
                            } else {
                                comment = '';
                            }
                            content = content.replace(/^\s*#.*$/gm, '');

                            var snpMap = {};
                            csv
                              .fromString(content, {delimiter: '\t'})
                              .on("record", function(row){
                                  if (/^rs\d+$/.test(row[0]) && /^[ATCG]{2}$/.test(row[3])) {
                                      snpMap[row[0]] = row[3];
                                  }
                              })
                              .on("end", function(){
                                  callback(null, {'snpMap': snpMap, 'comment': comment, 'count': Object.keys(snpMap).length});
                              });

                        };
                    }(records[i]);
                }


                async.parallel(tasks, function(err, results) {

                    var snpMaps = {};
                    for (i in results) {
                        snpMaps[i] = results[i].snpMap;
                        delete results[i].snpMap;
                    }

                    var db = dblite('../visualizations/snp-snip/databases/_genotype.db');
                    db.query('CREATE TABLE IF NOT EXISTS main (id INTEGER PRIMARY KEY, snpMaps TEXT)');
                    db.query('INSERT INTO main (snpMaps) VALUES (?)', [JSON.stringify(snpMaps)]);
                    db.query('SELECT last_insert_rowid()', function(rows) {
                        if (rows && rows[0]) {

                            results['_id'] = rows[0][0];
                            response.end(JSON.stringify(results));
                        }
                    });
                    db.close();

                });

            });
        });
                
    } else {
        // handle request for data about a SNP
        var rsNumber = querystring.parse(query).rs;
        var sessionId = querystring.parse(query).sessionId;

        
        var files = fs.readdirSync('../visualizations/snp-snip/databases').filter(function(filename){return /^\w+\.db$/.test(filename) && !/^_.*$/.test(filename)});
        var tasks = {};

        tasks['_genotype'] = function(callback) {
            dblite('../visualizations/snp-snip/databases/_genotype.db').query(
                'SELECT * FROM main WHERE id = ?',
                [sessionId],
                function(rows) {
                    if (rows && rows[0]) {

                        // remove the id column
                        rows[0].shift();

                        var snpMaps = JSON.parse(rows[0][0]);
                        var genotypes = {};

                        for (i in snpMaps) {
                            if (snpMaps[i].hasOwnProperty(rsNumber)) {
                                genotypes[i] = snpMaps[i][rsNumber];
                            }
                        }

                        callback(null, genotypes); 
                    } else {
                        callback(null, null);
                    }
                }
            );
        };

        for (i in files) {

            var resource = files[i].match(/^(\w+)\.db$/)[1];
            tasks[resource] = function(filename) { 
                return function(callback) {
                    dblite('../visualizations/snp-snip/databases/'+filename).query(
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
            for (resource in results) {
                if (results[resource] == null) {
                    delete results[resource];
                }
            }
            console.log(JSON.stringify(results));
            response.end(JSON.stringify(results));
        });
    }
}
    
exports.process = process;
