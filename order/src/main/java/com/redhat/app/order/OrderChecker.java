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

    @Inject
    TransactionRepository repo;

    @Scheduled(every="10s")     
    void checkOrder() {
        log.info("checking for stale orders....");
        List<Transaction> txList = repo.getIncompleteTransactions();
        log.info("***************************found stale tx "+txList.size());
        
    }

    //delete transaction records
    void onStop(@Observes ShutdownEvent ev) {               
        log.info("The application is stopping...");
        
        List<Transaction> txList = repo.listAll();
        for (Transaction transaction : txList) {
            
            repo.deleteAll();
        }
    }       
}