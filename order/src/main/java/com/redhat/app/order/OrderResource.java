package com.redhat.app.order;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;


import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.resteasy.annotations.SseElementType;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/rest")
public class OrderResource {

    @Inject
    @Channel("new-order")    
    Emitter<String> emitter;

    @Inject
    OrderService orderService;
    
    @Inject
    @Channel("status-stream") 
    Publisher<String> statusStream;         


    Logger log = LoggerFactory.getLogger(this.getClass());
    @POST
    @Path("/orders/submit")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Order submit(Order order) {
        return orderService.newOrder(order);
    }

    @GET
    @Path("/orders/status")
    @Produces(MediaType.SERVER_SENT_EVENTS)   
    @SseElementType("text/plain")
    public Publisher<String> getStatus() {
        log.info("Getting status update");
        return statusStream;

    }
}