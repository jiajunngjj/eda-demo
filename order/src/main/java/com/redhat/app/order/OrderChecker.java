package com.redhat.app.order;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.mongodb.panache.PanacheQuery;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.scheduler.Scheduled;

@ApplicationScoped
public class OrderChecker {
    Logger log = LoggerFactory.getLogger(this.getClass());

    @Inject
    OrderService orderService;

    @Scheduled(every="10s")     
    void checkOrder() {
        log.info("checking for stale orders....");
        List<Transaction> txList = Transaction.findInCompleteTransactions();
        log.info("***************************found stale tx "+txList.size());
        for (Transaction transaction : txList) {
            //TODO move this time check to mongodb query :P
            if (System.currentTimeMillis() - transaction.getDate().getTime() > 8000 ) {
                //transaction.setStatus("CANCELLED");
                //transaction.update(); 
             try {
                   orderService.cancelStaleTransactions(transaction.getOrder());
                    
                } catch (IllegalStateException ex) {
                    log.info("illegal stateexception thrown while checking order, ignoring");
                    //orderService.cancelStaleTransactions(transaction.getOrder());
                } catch (Exception ex) {
                    log.info("generic exception thrown while checking order, ignoring");
                    //orderService.cancelStaleTransactions(transaction.getOrder());
                }
            }
        }
    }

    //delete transaction records
    void onStop(@Observes ShutdownEvent ev) {               
        log.info("The application is stopping...");
        PanacheQuery<Transaction> q = Transaction.findAll();
        List<Transaction> txList = q.list();
        for (Transaction transaction : txList) {
            transaction.setStatus("CANCELLED");
            transaction.delete();
        }
    }       
}