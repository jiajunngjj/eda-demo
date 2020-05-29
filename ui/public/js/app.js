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
        formData["orderId"] = "order-"+Math.floor(Math.random() * 100000000)+"-"+formData["email"];
        console.log(JSON.stringify(formData));   
        //alert(JSON.stringify(formData));

        //submit form

    }
};