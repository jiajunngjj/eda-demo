var App = {
    test: function(msg) {
        console.log(msg);
    },
    clickSubmit: function() {
        var formData = {};
        var order = {};
        $.each($('#form1').serializeArray(), function() {
            formData[this.name] = this.value;
        });
        //formData["id"] = "order-"+Math.floor(Math.random() * 100000000)+"-"+formData["email"];
        formData["id"] = "ord-"+Math.floor(Math.random() * 10000);
        order["id"] = formData["id"];
        order["product"] = {};
        order["customer"] = {};
        order["product"]["id"] = formData["product"];
        order["customer"]["id"] = formData["customer"];
        order["customer"]["address"] = formData["address"];
        order["qty"] = formData["qty"];
        order["status"] = "NEW";
        console.log("order "+JSON.stringify(order));
        //submit form
        $.ajax({
            method: "POST",
            url: "./submit",
            data: formData,
            headers: {
              'Accept': 'text/event-stream'
            }
          })
            .done(function( msg ) {
              console.log( "received: " + msg );
              $('#response').html("Order submitted: "+msg);
              //$('#status').html("Order submitted: "+msg);
            });        

    }
};
