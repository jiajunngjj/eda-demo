var express = require('express');
var router = express.Router();
const  http = require('http');


/* GET home page. */
router.get('/', function(req, res, next) {
  res.render('index.html', { title: 'Express' });
});
router.post('/submit', function(req, res, next) {
  console.log(req.body);
  console.log(req.body.orderId);

  var post_options = {
      host: "192.168.0.110",
      port: "8080",
      path: "/rest/orders/submit",
      method: "POST",
      headers: {
          "Content-Type": "application/json"
      }
  };

  var post_req = http.request(post_options, function(post_res) {
      post_res.setEncoding("utf8");
      post_res.on('data', function (chunk) {
          console.log('Response: ' + chunk);
      });
      post_res.on('end', function () {
          res.send(req.body.id);
      });

  });

  post_req.write(JSON.stringify(req.body));
  post_req.end();
});
module.exports = router;
