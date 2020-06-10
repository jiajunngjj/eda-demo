package com.redhat.app.inventory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Named;

@ApplicationScoped
@ActivateRequestContext
@Named("InProgressSender")
public class InProgressSender extends MessageSender{
    
    public InProgressSender() {
        super("order-in-progress");
    } 
}