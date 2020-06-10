package com.redhat.app.inventory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.Queue;
import javax.jms.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class MessageSender {
    
    @Inject
    ConnectionFactory connectionFactory;

    JMSContext context;
    Queue queue;
    JMSProducer producer;
    Logger log = LoggerFactory.getLogger(this.getClass());
    public MessageSender() {

    }
    String queueName;
    public MessageSender(String name) {
        this.queueName = name;
    }
    public void send (String msg) {
        try {
            log.info(connectionFactory+"");
            context = connectionFactory.createContext(Session.AUTO_ACKNOWLEDGE);
            producer = context.createProducer();
            queue = context.createQueue(queueName);            
            producer.send(queue,msg);

        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        
    }
    
}