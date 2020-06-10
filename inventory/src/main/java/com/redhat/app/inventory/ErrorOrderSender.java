package com.redhat.app.inventory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Named;

@ApplicationScoped
@ActivateRequestContext
@Named("ErrorOrderSender")
public class ErrorOrderSender extends MessageSender{

    public ErrorOrderSender() {
        super("order-error");
    }
    
}