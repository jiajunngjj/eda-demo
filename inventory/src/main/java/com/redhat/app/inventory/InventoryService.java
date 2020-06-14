package com.redhat.app.inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;

import com.google.gson.Gson;
import com.redhat.app.inventory.status.InventoryStatus;
import com.redhat.app.inventory.status.OrderStatus;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.OnOverflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;



@ApplicationScoped
@ActivateRequestContext
public class InventoryService {

    private static Map<OrderStatus,List<Order>> records;
    Gson gson = new Gson();
    Logger log = LoggerFactory.getLogger(this.getClass());
    @Inject
    InventoryRepository repo;


    //amqp
    /*
    @Inject
    @Named("InProgressSender")
    MessageSender inprogressSender ;

    @Inject
    @Named("ErrorOrderSender")
    MessageSender errorOrderSender;
    */


    //private final ExecutorService scheduler = Executors.newSingleThreadExecutor();
    private final ExecutorService scheduler = Executors.newFixedThreadPool(10);


    private final Object lock = new Object();
    //reactive
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

    //Possible incomeing usecase : NEW
    //Outgoing : INVENTORY_UPDATED, INNVENTORY_INSUFFICIENT_STOCK



    void startUp(@Observes StartupEvent ev) {
        records = new HashMap<OrderStatus, List<Order>>();        
    }

    void shutdown(@Observes ShutdownEvent ev) {
        this.info();
    }
    @Transactional
    @Incoming("order-new")
    public Order processNewOrder(String json) {
        //simulate processing inventory
        
        Order order = gson.fromJson(json, Order.class);   
        log.info("*******Incoming NEW ORDER "+order);
        boolean sent=false;
        try {

                updateInventory(order,-1,repo);
                //order.setStatus("INVENTORY_UPDATED");
                order.setStatus(OrderStatus.IN_PROGRESS);
                order.setInventoryStatus(InventoryStatus.UPDATED);
                json = gson.toJson(order);

                inprogressEmitter.send(json);
                sent=true;
                //inprogressSender.send(json);   

        } catch (InventoryException e) {
            log.info(e.getMessage()+" NEW ORDER error thrown");
            //order.setStatus(e.getMessage());
            //order.setInventoryStatus(e.getMessage());
            order.setInventoryStatus(InventoryStatus.NO_STOCK);
            json = gson.toJson(order);
            log.info("SENDING TO ERROR "+json);
        } catch (Exception e) {
            //e.printStackTrace();
            log.info(e.getMessage()+" NEW ORDER error thrown");
            //order.setStatus(e.getMessage());
            order.setStatus(OrderStatus.ERROR_PROCESSING);
            json = gson.toJson(order);
        }
        finally {
            //
            if (!sent) {

                log.info("SENDING TO ERROR "+json);
                try {
                errorEmitter.send(json);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
    
            }
        }

        return order;
    }


    //receive errors related to inventory, for now only handle insufficient stock
    //flow ends here, we may want to return a message to orderservice
    
    @Incoming("order-error-inv")
    @Transactional
    public void processError(String json) throws InventoryException{
        log.info("*******INCOMING INV ERROR ----- "+json);
    try{
        Order order = gson.fromJson(json, Order.class);

        if (order ==null || order.getStatus()==null) {
            //log.info("order = null, corrupted data ");
            return;
        } else 
//        if ( order.getStatus().equals("INVENTORY_INSUFFICIENT_STOCK")) {
        if ( order.getInventoryStatus().equals(InventoryStatus.NO_STOCK)) {

            //log.info("Insufficient stock error, nothng to do, did not reduce stock in the first place");
            return;
        }
        else {
            //dirty hack - simple scenario, add back the reduced qty
            log.info("detected Error in order txn: "+order.getId()+" , reverting inventory");
            
           
            Order o = updateInventory(order,1,repo);
            //going on where from here, just update for completeness
            //o.setStatus("INVENTORY_REVERTED ");
            //o.setInventoryStatus("INVENTORY_REVERTED ");
            o.setStatus(OrderStatus.CANCELLED);
            o.setInventoryStatus(InventoryStatus.REVERTED);
            //
            
        }    

    }catch (Exception ex) {
        log.info(ex.getMessage()+" Error ORDER error thrown");

        //ex.printStackTrace();

    }
    }   



    @Transactional
     private Order updateInventory(Order order, int type, InventoryRepository repo) throws InventoryException, InterruptedException, ExecutionException{
        synchronized(lock) {
                if (records.get(order.getStatus()) != null) {

                    records.get(order.getStatus()).add(order);
                } else {

                    records.put(order.getStatus(), new ArrayList<Order>());
                    records.get(order.getStatus()).add(order);

                }

                //scheduler.execute(new DBService(order, type, repo));

                Future<Order> future = scheduler.submit(new DBService(order, type, repo));
                Order returnOrder = future.get();
                log.info("returned from callable "+returnOrder);
                //if (returnOrder.getStatus().equals("INVENTORY_INSUFFICIENT_STOCK")) {
                    if (returnOrder.getInventoryStatus().equals(InventoryStatus.NO_STOCK)) {
                    //errorEmitter.send(gson.toJson(returnOrder));
                    log.info("Not enough stocks..... "+returnOrder);
                    throw new InventoryException("INVENTORY_INSUFFICIENT_STOCK");
                }
            return returnOrder;
        }
    }


    public void info() {
        Iterator<OrderStatus> itr = InventoryService.records.keySet().iterator();

        while (itr.hasNext()) {
            
            OrderStatus status = itr.next();
            //log.info(">>>>> Status: "+status);
            List<Order> orders = records.get(status);
            log.info(">>>>> Status: "+status+": count "+orders.size());

        }

    }
}
