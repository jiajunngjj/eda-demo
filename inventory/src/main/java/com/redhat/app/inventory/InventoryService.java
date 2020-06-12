package com.redhat.app.inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import com.google.gson.Gson;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.OnOverflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class InventoryService {
    Gson gson = new Gson();


    @Inject
    @Channel("order-in-progress")//in-progress-order
    //@OnOverflow(value=OnOverflow.Strategy.BUFFER, bufferSize=512)                
    //@OnOverflow(OnOverflow.Strategy.LATEST)
    @OnOverflow(OnOverflow.Strategy.UNBOUNDED_BUFFER)
    Emitter<String> inprogressEmitter;

    @Inject
    @Channel("order-error")
    //@OnOverflow(value=OnOverflow.Strategy.BUFFER, bufferSize=512)                
    //@OnOverflow(OnOverflow.Strategy.LATEST)                
    @OnOverflow(OnOverflow.Strategy.UNBOUNDED_BUFFER)
    Emitter<String> errorEmitter;    

    @ConfigProperty(name = "app.error.inventory.id")
    String injectedError;

    Logger log = LoggerFactory.getLogger(this.getClass());
    //For debugging , to be removed
    static int incomingError = 0;
    static int revertingCount = 0;
    static int normalInventoryUpdate =0;
    static int sushicount =0;
    static int bugercount =0;
    @Incoming("order-new")
    //Possible incomeing usecase : NEW
    //Outgoing : INVENTORY_UPDATED, INNVENTORY_INSUFFICIENT_STOCK
    public Order processNewOrder(String json) {
        //simulate processing inventory
        
        Order order = gson.fromJson(json, Order.class);   
        log.info("*******Received new order "+order);
        try {
                //Thread.sleep(1000);
                //simulate delay in processing
                this.updateInventory(order);
                order.setStatus("INVENTORY_UPDATED");
                json = gson.toJson(order);
                //log.info("Done updating inventory, sending to in progress emitter "+order);

                inprogressEmitter.send(json);
    
           
		} catch (InventoryException e) {
            //e.printStackTrace();
            log.info("Inventory Error captured "+e.getMessage());
            order.setStatus(e.getMessage()); //Should be insufficient stock        
            json = gson.toJson(order);
            //log.info("sending to order error queue "+order);
            errorEmitter.send(json);
        } catch (Exception ex) {
            //for any illegal state exceptions
            //log.info("Exception "+ex);
            
            //resend
            errorEmitter.send(json);
        }

        //catch (InterruptedException e) {
          //  e.printStackTrace();
       // } 

        return order;
    }

    //receive errors related to inventory, for now only handle insufficient stock
    //flow ends here, we may want to return a message to orderservice
    @Incoming("order-error-inv")
    @Transactional
    public void processError(String json) {
        log.info("*******INCOMING INV ERROR ----- "+json);
        log.info("*******COUNT:"+(InventoryService.incomingError+=1));
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
            
            //for mongodb
            Inventory i =Inventory.findById(order.getProduct());
            i.setStock(i.getStock()+order.getQty());
            i.update();
            log.info("************Reverting inventory "+(InventoryService.revertingCount+=1));
            log.info("*************"+i.getName()+":"+i.getStock());
            order.setStatus("INVENTORY_REVERTED ");
            //json = gson.toJson(order);
            //inprogressEmitter.send(json);
        }    
    }   
    //to update inventory of stock, if qty ordered is higher, throw exception and return exp
    // to calling method which is process(new order). a message will be returned to orderservice
    @Transactional
    private Order updateInventory(Order order) throws InventoryException{
        //mongodb
        Inventory i =Inventory.findById(order.getProduct());
        
        //log.info("find by id "+i);
    
        if (i !=null && order.getQty() !=null) {
            //log.info("---Update inventory with order "+order+"| inv:"+i.getStock());
            if ( (i.getStock().intValue() < order.getQty().intValue())) {
                throw new InventoryException("INVENTORY_INSUFFICIENT_STOCK");
            }

            i.setStock(Integer.valueOf(i.getStock().intValue() - order.getQty().intValue()));
            i.update();


            log.info("*********Updating Inventory normal flow :"+(InventoryService.normalInventoryUpdate+=1));
            if (i.getName().equals("Sushi")) {
                log.info("*********Updating Inventory normal flow sushi:"+(InventoryService.sushicount+=1)+"**"+i.getName()+":"+i.getStock());
            }else {
                log.info("*********Updating Inventory normal flow burger  :"+(InventoryService.bugercount+=1)+"**"+i.getName()+":"+i.getStock());                
            }
            
            //log.info("---Updated inventory with order "+order+"| inv:"+i.getStock());
            //log.info("***********************************************************************************");
            //log.info("*********"+order.getId()+"****"+i.getStock()+"*************************************");
            //log.info("***********************************************************************************");
            

        }
        return order;
    }
}
