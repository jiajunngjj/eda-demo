package com.redhat.app.order;

import java.util.Date;
import java.util.List;

import com.redhat.app.order.status.InventoryStatus;
import com.redhat.app.order.status.OrderStatus;
import com.redhat.app.order.status.TransactionStatus;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import lombok.Data;
@Data
public class Transaction extends PanacheMongoEntity{
    
    String id;
    Order order;
    Date date=new Date();

    TransactionStatus status = TransactionStatus.NEW;
    /*
    String status="NEW";
    String paymentStatus="COMPLETED";
    String inventoryStatus="NEW";
    String deliveryStatus="COMPLETED";
    */
    Boolean isOrderComplete() {
        if (order.getStatus().equals(OrderStatus.CONFIRMED)) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    public static List<Transaction> findInCompleteTransactions(){
        return list("status", "NEW");
        //return list("{ status: { $ne: ?1 }, status: { $ne: ?2 } } ", "COMPLETED", "CANCELLED");
    }
    
}