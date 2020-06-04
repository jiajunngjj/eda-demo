var App = {
    test: function(msg) {
        console.log(msg);
    },
    clickSubmit: function() {
        var formData = {};
        //does not work for checkbox and radios
        $.each($('#form1').serializeArray(), function() {
            formData[this.name] = this.value;
        });
        //formData["id"] = "order-"+Math.floor(Math.random() * 100000000)+"-"+formData["email"];
        formData["id"] = "ord-"+Math.floor(Math.random() * 10000);
        console.log(JSON.stringify(formData));   
        //alert(JSON.stringify(formData));

        //submit form
        $.ajax({
            method: "POST",
            url: "./submit",
            data: formData
          })
            .done(function( msg ) {
              console.log( "received: " + msg );
              //$('#response').html("Order submitted: "+msg);
              //$('#status').html("Order submitted: "+msg);
            });        

    }, stream:function() {
            //submit form
            $.ajax({
              method: "GET",
              url: "./stream"
            })
              .done(function( msg ) {
                console.log( "received: " + msg );
                return msg;
              });  
    }

};
