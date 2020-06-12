package com.redhat.app.inventory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class NewOrderMessageConsumer implements Runnable, MessageListener{
    @Inject
    ConnectionFactory connectionFactory;


    public NewOrderMessageConsumer() {
        log.info("CONSTRUCTOR "+this.connectionFactory);
    }

    //private final ExecutorService scheduler = Executors.newSingleThreadExecutor();
    private final ExecutorService scheduler = Executors.newFixedThreadPool(50);
    void onStart(@Observes StartupEvent ev) {
        log.info("Onstart "+this.connectionFactory);
        context = connectionFactory.createContext(Session.SESSION_TRANSACTED);
        //scheduler.submit(this);
        
        
    }

    void onStop(@Observes ShutdownEvent ev) {
        scheduler.shutdown();
    }
    @Inject
    InventoryService inventoryService;


    @Override
    public void onMessage(Message message) {
        String json;
		try {
            log.info(" inside "+Thread.currentThread().getId()+"-"+(++cnt));

            log.info("message "+((TextMessage)message).getText());
            json = ((TextMessage)message).getText();
            
            log.info("Integration Service "+inventoryService);            
            this.inventoryService.processNewOrder(json);    
            log.info("called service");
            

        }       
        catch (Exception e) {
			e.printStackTrace();
        }   
    }
    private JMSContext context ;
    int cnt = 0;
    Logger log = LoggerFactory.getLogger(this.getClass());
    @Override
    public void run() {
        JMSConsumer newOrder =null;
        try  
        {
            newOrder = context.createConsumer(context.createQueue("order-new"));
            while(true) {
                //log.info(" inside consumer "+Thread.currentThread().getId()+"-"+(cnt++));
                //newOrder.setMessageListener(this);

                Thread.sleep(500);
            context.commit();

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            log.info("finally ");
            newOrder.close();
            context.close();
        }
    }
}
