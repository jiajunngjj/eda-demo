package com.redhat.app.order;

import java.util.concurrent.Callable;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Data;
@Data
@ApplicationScoped
@ActivateRequestContext
public class TransactionDBService implements Callable<Transaction> {

    Logger log = LoggerFactory.getLogger(this.getClass());

    private Order order;
    private String type;
    private TransactionRepository repo;
    public TransactionDBService(String type, Order order, TransactionRepository repo) {
        this.type = type;
        this.order = order;
        this.repo = repo;
    }

    public TransactionDBService() {

    }

    @Override
    public Transaction call() throws Exception {
        Transaction t = null;
        if (this.type.equals("INVENTORY")) {
            return t = repo.updateInventoryStatus(order);
        }
        else if (this.type.equals("DELIVERY")) {
            return t=repo.updateDeliveryStatus(order);
        }
        else if (this.type.equals("CANCEL")) {

            return t=repo.cancelTransaction(order);
        }
        else if (this.type.equals("COMPLETE")) {
            return t = repo.completeTransaction(order);
        }
        else { // no type, empty string
            return t=repo.createTransaction(order);
        }
    }
 
}