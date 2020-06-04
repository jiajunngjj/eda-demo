var express = require('express');
var router = express.Router();
const  http = require('http');


/* GET home page. */
router.get('/', function(req, res, next) {
  res.render('index.html', { title: 'Express', host: process.env.URL });
});
router.get('/dashboard', function(req, res, next) {
    res.render('dashboard.html', { host: process.env.URL });
  });
router.get('/stream', function(req,res,next){
    res.setHeader('Cache-Control', 'no-cache');
    res.setHeader('Content-Type', 'text/event-stream');
    res.setHeader("Access-Control-Allow-Origin", "*");
    res.flushHeaders(); // flush the headers to establish SSE with client

    //let interValID = setInterval(() => {
        const options = {
            host: 'order-eda-demo.apps.cluster-sgp-ca65.sgp-ca65.example.opentlc.com',
            port: 80,
            path: '/rest/orders/status',
            method: 'GET',
            headers: {
              'Accept': 'text/event-stream'
            }
          };

          var httpreq = http.get(options, function(httpres) {
            //console.log('STATUS: ' + res.statusCode);
            //console.log('HEADERS: ' + JSON.stringify(res.headers));
          
            // Buffer the body entirely for processing as a whole.
            //var bodyChunks = [];
            httpres.on('data', function(chunk) {
              // You can process streamed parts here...
              //bodyChunks.push(chunk);
              res.write(chunk);
            }).on('end', function() {
              //var body = Buffer.concat(bodyChunks);
              console.log('end');
            })
          });

        //res.write(`data: ${JSON.stringify({num: counter})}\n\n`); // res.write() instead of res.send()
   // }, 3000);    
} );
router.post('/submit', function(req, res, next) {
  console.log(req.body);
  console.log(req.body.orderId);

  var post_options = {
      host: process.env.BACKEND_URL || "192.168.0.110",
      port: process.env.BACKEND_PORT || "8080",
      path: "/rest/orders/submit",
      method: "POST",
      headers: {
          "Content-Type": "application/json"
      }
  };

  var post_req = http.request(post_options, function(post_res) {
      post_res.setEncoding("utf8");
      post_res.on('data', function (chunk) {
          //console.log('Response: ' + chunk);
      });
      post_res.on('end', function () {
          res.send(req.body.id);
      });

  });

  post_req.write(JSON.stringify(req.body));
  post_req.end();
});
module.exports = router;
