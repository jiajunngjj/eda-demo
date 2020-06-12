package com.redhat.app.inventory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;

import com.google.gson.Gson;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.OnOverflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



@ApplicationScoped
@ActivateRequestContext
public class InventoryService {
    Gson gson = new Gson();
    Logger log = LoggerFactory.getLogger(this.getClass());
    @Inject
    InventoryRepository repo;


    @Inject
    @Named("InProgressSender")
    MessageSender inprogressSender ;

    @Inject
    @Named("ErrorOrderSender")
    MessageSender errorOrderSender;
    //For debugging , to be removed
    static int incomingError = 0;
    static int revertingCount = 0;
    static int normalInventoryUpdate =0;
    static int sushicount =0;
    static int bugercount =0;



    //@Inject
    //@Channel("order-in-progress")//in-progress-order
    //@OnOverflow(value=OnOverflow.Strategy.BUFFER, bufferSize=512)                
    //@OnOverflow(OnOverflow.Strategy.LATEST)
    //@OnOverflow(OnOverflow.Strategy.UNBOUNDED_BUFFER)
    //Emitter<String> inprogressEmitter;
        
    //@Inject
    //@Channel("order-error")
    //@OnOverflow(value=OnOverflow.Strategy.BUFFER, bufferSize=512)                
    //@OnOverflow(OnOverflow.Strategy.LATEST)                
    //@OnOverflow(OnOverflow.Strategy.UNBOUNDED_BUFFER)
    //Emitter<String> errorEmitter;  

    //Possible incomeing usecase : NEW
    //Outgoing : INVENTORY_UPDATED, INNVENTORY_INSUFFICIENT_STOCK

    @Transactional
    //@Incoming("order-new")
    public Order processNewOrder(String json) {
        //simulate processing inventory
        
        Order order = gson.fromJson(json, Order.class);   
        log.info("*******Received new order "+order);
        try {

                
                updateInventory(order);
                order.setStatus("INVENTORY_UPDATED");
                json = gson.toJson(order);

                inprogressSender.send(json);
                

        //} catch (InventoryException e) {
        } catch (Exception e) {
            e.printStackTrace();
            //log.info(e.getMessage()+" error thrown");
            order.setStatus(e.getMessage());
            json = gson.toJson(order);
            

            errorOrderSender.send(json);
        }

        return order;
    }


    //receive errors related to inventory, for now only handle insufficient stock
    //flow ends here, we may want to return a message to orderservice
    
    //@Incoming("order-error-inv")
    @Transactional
    public void processError(String json) throws InventoryException{
        log.info("*******INCOMING INV ERROR ----- "+json);
        log.info("*******COUNT:"+(InventoryService.incomingError+=1));
    try{
        Order order = gson.fromJson(json, Order.class);

        if (order ==null || order.getStatus()==null) {
            //log.info("order = null, corrupted data ");
            return;
        } else 
        if ( order.getStatus().equals("INVENTORY_INSUFFICIENT_STOCK")) {
            //log.info("Insufficient stock error, nothng to do, did not reduce stock in the first place");
            return;
        }
        else {
            //dirty hack - simple scenario, add back the reduced qty
            log.info("detected Error in order txn: "+order.getId()+" , reverting inventory");
            
           
            Inventory i = repo.updateStock(order,1);
            order.setStatus("INVENTORY_REVERTED ");
            log.info("************Reverting inventory "+(InventoryService.revertingCount+=1));
            log.info("*************"+i.getName()+":"+i.getStock());            
            
        }    

    }catch (Exception ex) {
        ex.printStackTrace();
    }
    }   



    @Transactional
     private Order updateInventory(Order order) throws InventoryException{

            Inventory i = repo.updateStock(order,-1);
            log.info("stock after update:"+i.getName()+":"+i.getName());
        
            log.info("*********Updating Inventory normal flow :"+(InventoryService.normalInventoryUpdate+=1));
            if (i.getName().equals("Sushi")) {
                log.info("*********Updating Inventory normal flow sushi:"+(InventoryService.sushicount+=1)+"**"+i.getName()+":"+i.getStock());
            }else {
                log.info("*********Updating Inventory normal flow burger  :"+(InventoryService.bugercount+=1)+"**"+i.getName()+":"+i.getStock());                
            }
        return order;
    }
}
