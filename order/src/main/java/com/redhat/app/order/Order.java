package com.redhat.app.order;

import com.redhat.app.order.status.DeliveryStatus;
import com.redhat.app.order.status.InventoryStatus;
import com.redhat.app.order.status.OrderStatus;

import lombok.Data;

@Data
public class Order {

    String id;
    String customer;
    String product;
    String address;
    String email;
    Integer qty;
    OrderStatus status=OrderStatus.NEW;
    DeliveryStatus deliveryStatus=DeliveryStatus.NEW;
    InventoryStatus inventoryStatus=InventoryStatus.NEW;
    String paymentStatus="NEW";
    /**
     Product product;
     Customer customer;
     Integer qty;
    */


}
