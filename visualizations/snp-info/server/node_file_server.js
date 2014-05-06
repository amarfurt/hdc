var connect = require('connect');
var http = require('http');


connect()
    .use(function(req, res, next){
        res.setHeader("Access-Control-Allow-Origin", "*");
        return next();
    })
    .use(connect.static('snp_db'))
    .listen(8888);

console.log('Listening on port 8888.');
