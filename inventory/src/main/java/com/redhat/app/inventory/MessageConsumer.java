package com.redhat.app.inventory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class MessageConsumer implements Runnable{
    @Inject
    ConnectionFactory connectionFactory;

    @Inject
    InventoryService inventoryService;

    private final ExecutorService scheduler = Executors.newSingleThreadExecutor();
    
    void onStart(@Observes StartupEvent ev) {
        scheduler.submit(this);
        
    }

    void onStop(@Observes ShutdownEvent ev) {
        scheduler.shutdown();
    }

    Logger log = LoggerFactory.getLogger(this.getClass());
    @Override
    public void run() {
        try (
            
            JMSContext context = connectionFactory.createContext(Session.AUTO_ACKNOWLEDGE)) 
            {
            JMSConsumer newOrder = context.createConsumer(context.createQueue("order-new"));
               
            while (true) {

                Message message = newOrder.receive(1000);
                
                if (message!= null) {

                log.info("got message "+((TextMessage)message).getText());
                inventoryService.processNewOrder(((TextMessage)message).getText());
                log.info("called service");
                //Thread.sleep(500);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}