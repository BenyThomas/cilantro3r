package com.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.repository.Recon_M;
import jakarta.servlet.annotation.WebListener;
import org.jpos.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@Configuration
@WebListener
public class PushThread implements Runnable, org.jpos.util.LogSource {

    @Autowired
    @Qualifier("dbPoolExecutor")
    ThreadPoolTaskExecutor exec;

    @Autowired
    ObjectMapper jacksonMapper;

    @Autowired
    Recon_M reconRepo;
    String name;
    String menu_variable;
//    @Autowired
//    HttpSession httpSession;

    String txnType;
    String txndate;
    String username;
    Logger logger = null;
    String realm = "tigoPush";
    LogEvent logEvent = new LogEvent(this, "transaction");
    Caller caller = new Caller();

    public void setName(String name) {
        this.name = name;
    }

    public void setMenuVariable(String menu_variable) {
        this.menu_variable = menu_variable;
    }

    @Override
    public void run() {
        reconRepo.initiateRecon(getTxndate(), getTxnType(), getUsername());
        reconRepo.initiateReconExcetpions(getTxndate(), getTxnType(), getUsername());
    }

    @Override
    public void setLogger(Logger logger, String realm) {
        this.logger = logger;
        this.realm = realm;
    }

    @Override
    public String getRealm() {
        return realm;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    public String getTxnType() {
        return txnType;
    }

    public void setTxnType(String txnType) {
        this.txnType = txnType;
    }

    public String getTxndate() {
        return txndate;
    }

    public void setTxndate(String txndate) {
        this.txndate = txndate;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

}
