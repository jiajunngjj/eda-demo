var createError = require('http-errors');
var express = require('express');
var path = require('path');
var cookieParser = require('cookie-parser');
var logger = require('morgan');

var indexRouter = require('./routes/index');
var usersRouter = require('./routes/users');

var app = express();

// view engine setup
app.set('views', path.join(__dirname, 'views'));
app.set('view engine', 'ejs');

app.use(logger('dev'));
app.use(express.json());
app.use(express.urlencoded({ extended: false }));
app.use(cookieParser());
app.use(express.static(path.join(__dirname, 'public')));

app.use('/', indexRouter);
app.use('/users', usersRouter);

// catch 404 and forward to error handler
app.use(function(req, res, next) {
  next(createError(404));
});

// error handler
app.use(function(err, req, res, next) {
  // set locals, only providing error in development
  res.locals.message = err.message;
  res.locals.error = req.app.get('env') === 'development' ? err : {};

  // render the error page
  res.status(err.status || 500);
  res.render('error');
});


//Start my amqp listener here
var container = require('rhea');
var deliveryService = require('./business/delivery');

const amqpOptions = {
  'host': '192.168.0.110', 
  'port': 7672,
  'username': 'admin',
  'password': 'admin'
}

container.on("sender_open", function (event) {
  console.log("SEND: Opened sender for target address '" +
              event.sender.target.address + "'");
});

container.on("receiver_open", function (event) {
  console.log("RECEIVE: Opened receiver for source address '" +
              event.receiver.source.address + "'");
});

container.on('message', function (event) {
  console.log("RECV DELIVERY EVENT");
  console.log(event.message.body);

  var result=  deliveryService.process(event.message.body);
  console.log(result);
  send(JSON.stringify(result));
  //event.connection.close();
});

container.on("sendable", function (event) {
  var message = {
      body: messageBody
  };

  event.sender.send(message);

  console.log("SEND: Sent message '" + message.body + "'");

  event.sender.close();
  event.connection.close();
});

container.on('accepted', function (event) {
  console.log('all messages confirmed');
  event.connection.close();

});
container.on('disconnected', function (event) {
  if (event.error) console.error('%s %j', event.error, event.error);
});


var connection = container.connect(amqpOptions);
connection.open_receiver('order-new-delivery');



var messageBody="";
function send(msg) {
  messageBody = msg;
  connection.open_sender('order-in-progress');
}


module.exports = app;
