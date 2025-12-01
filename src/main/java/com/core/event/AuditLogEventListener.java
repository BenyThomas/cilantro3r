/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.core.event;
import com.repository.User_M;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * @author samichael
 */
@Component
public class AuditLogEventListener {
    @Autowired
    User_M userRepo;    
    private static final Logger LOGGER = LoggerFactory.getLogger(AuditLogEventListener.class);

    @Async("auditTrail")
    @EventListener
    public void handleAuditLogEvent(AuditLogEvent ev) {
        LOGGER.trace("AuditLogEvent is running..... username: {}, action: {}, status: {}, all = {}",ev.getUsername(),ev.getComments(),ev.getStatus(),ev);
        this.userRepo.logAudit(ev);
    }
}
