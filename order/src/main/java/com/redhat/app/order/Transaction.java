package com.redhat.app.order;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import com.redhat.app.order.status.InventoryStatus;
import com.redhat.app.order.status.OrderStatus;
import com.redhat.app.order.status.TransactionStatus;

import lombok.Data;
@Data
@Entity
public class Transaction {
    
    @Id
    String id;
    @OneToOne(cascade = CascadeType.ALL)
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
        if (
            order.getStatus().equals(OrderStatus.CONFIRMED)
            && order.getInventoryStatus().equals(InventoryStatus.CONFIRMED)
        
        ) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    /*
    public static List<Transaction> findInCompleteTransactions(){
        return list("status", "NEW");
        //return list("{ status: { $ne: ?1 }, status: { $ne: ?2 } } ", "COMPLETED", "CANCELLED");
    }
    */
    
}
