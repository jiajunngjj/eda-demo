package com.redhat.app.payment;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class PaymentService {

    Logger log = LoggerFactory.getLogger(this.getClass());

    @Incoming("new-payment")
    public String paymentProcess(String json) {
        log.info("Payment done for "+ json);
        return json;
    }

//    @Inject
//    @Channel("payment-error")
//    Emitter<String> emitter;


}
