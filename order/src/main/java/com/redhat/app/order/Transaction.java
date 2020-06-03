package com.redhat.app.order;

import java.util.Date;
import java.util.List;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import lombok.Data;
@Data
public class Transaction extends PanacheMongoEntity{
    
    String id;
    Order order;
    Date date=new Date();
    String status="NEW";
    String paymentStatus="COMPLETED";
    String inventoryStatus="NEW";
    String deliveryStatus="COMPLETED";

    Boolean isOrderComplete() {
        if (deliveryStatus.equals("COMPLETED") 
        && deliveryStatus.equals("COMPLETED") 
        && deliveryStatus.equals("COMPLETED")) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    public static List<Transaction> findInCompleteTransactions(){
        return list("status", "NEW");
    }
    
}