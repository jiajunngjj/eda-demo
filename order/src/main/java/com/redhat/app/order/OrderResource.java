package com.redhat.app.order;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;


import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/rest")
public class OrderResource {

    @Inject
    @Channel("new-order")    
    Emitter<String> emitter;

    @Inject
    OrderService orderService;
    
    Logger log = LoggerFactory.getLogger(this.getClass());
    @POST
    @Path("/orders/submit")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Order submit(Order order) {

        return orderService.newOrder(order);
    }
}