package com.redhat.app.order;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.persistence.LockModeType;
import javax.transaction.Transactional;

import com.redhat.app.order.status.InventoryStatus;
import com.redhat.app.order.status.OrderStatus;
import com.redhat.app.order.status.TransactionStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.hibernate.orm.panache.PanacheRepository;

@ApplicationScoped
@ActivateRequestContext
public class TransactionRepository implements PanacheRepository<Transaction> {
    
    Logger log = LoggerFactory.getLogger(this.getClass());

    @Inject
    OrderService orderService;

    @Transactional
    public List<Transaction> getIncompleteTransactions() {
        List<Transaction> list = find("status", TransactionStatus.NEW).withLock(LockModeType.PESSIMISTIC_WRITE).list();
        log.info("found "+list.size()+" transactions for processing");
        //List<Order> revertInventory= new ArrayList<Order);
        for (Transaction transaction : list) {
            if (System.currentTimeMillis() - transaction.getDate().getTime() > 5000 ) {
             try {
                   //orderService.cancelStaleTransactions(transaction.getOrder());
                   transaction.setStatus(TransactionStatus.CANCELLED);
                   if (transaction.getOrder().getInventoryStatus().equals(InventoryStatus.UPDATED) || transaction.getOrder().getInventoryStatus().equals(InventoryStatus.CONFIRMED) ) {
                       //revertInventory.add(transaction.getOrder());
                        orderService.revertInventory(transaction.getOrder());
                   }
                   transaction.getOrder().setStatus(OrderStatus.CANCELLED);
                   orderService.sendCancelStatus(transaction.getOrder());
                   this.persistAndFlush(transaction);
                } catch (IllegalStateException ex) {
                    log.info("illegal stateexception thrown while checking order, ignoring");
                    //orderService.cancelStaleTransactions(transaction.getOrder());
                } catch (Exception ex) {
                    log.info("generic exception thrown while checking order, ignoring");
                    //orderService.cancelStaleTransactions(transaction.getOrder());
                }
            }
        }//for        
        return list; 
    }

    @Transactional
    public Transaction updateInventoryStatus(Order order) {

        //Transaction t = find("name",order.getProduct()).withLock(LockModeType.PESSIMISTIC_WRITE).firstResult();
        Transaction t = find("id",order.getId()).withLock(LockModeType.PESSIMISTIC_WRITE).firstResult();
        t.getOrder().setInventoryStatus(order.getInventoryStatus());
        this.persistAndFlush(t);
        return t;
    }

    @Transactional
    public Transaction updateDeliveryStatus(Order order) {

        //Transaction t = find("name",order.getProduct()).withLock(LockModeType.PESSIMISTIC_WRITE).firstResult();
        Transaction t = find("id",order.getId()).withLock(LockModeType.PESSIMISTIC_WRITE).firstResult();
        t.getOrder().setDeliveryStatus(order.getDeliveryStatus());
        this.persistAndFlush(t);
        return t;
    }    

    @Transactional
    public Transaction createTransaction(Order order) {
        Transaction t = new Transaction();
        t.setId(order.getId());
        t.setDate(new Date());
        t.setStatus(TransactionStatus.NEW);
        t.setOrder(order);
        this.persistAndFlush(t);
        return t;
    }

    @Transactional
    public Transaction cancelTransaction(Order order) {
        Transaction t = find("id",order.getId()).withLock(LockModeType.PESSIMISTIC_WRITE).firstResult();
        if (!t.getStatus().equals(TransactionStatus.COMPLETED)) {
            t.getOrder().setStatus(OrderStatus.CANCELLED);
            t.setStatus(TransactionStatus.CANCELLED);
        }
        this.persist(t);
        return t;
    }
    

    @Transactional
    public Transaction completeTransaction(Order order) {
        Transaction t = find("id",order.getId()).withLock(LockModeType.PESSIMISTIC_WRITE).firstResult();
        if (!t.getOrder().getStatus().equals(OrderStatus.CANCELLED) && !t.getStatus().equals(TransactionStatus.CANCELLED)) {
            t.getOrder().setStatus(OrderStatus.CONFIRMED);
            t.setStatus(TransactionStatus.COMPLETED);
        }
        this.persist(t);
        return t;
    }    
}