/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.core.event;

/**
 *
 * @author melleji.mollel
 */
public class AuditLogEvent {

    private final String username;
    private final String roleId;
    private final String branchNo;
    private final String ip_address;
    private final String function_name;
    private final String status;
    private final String comments;

    public AuditLogEvent(String username, String roleId, String branchNo, String ip_address, String function_name, String status, String comments) {
        this.username = username;
        this.roleId = roleId;
        this.branchNo = branchNo;
        this.ip_address = ip_address;
        this.function_name = function_name;
        this.status = status;
        this.comments = comments;
    }

    public String getUsername() {
        return username;
    }

    public String getRoleId() {
        return roleId;
    }

    public String getBranchNo() {
        return branchNo;
    }

  

    public String getIpAddress() {
        return ip_address;
    }

    public String getFunctionName() {
        return function_name;
    }

 

    public String getStatus() {
        return status;
    }

    public String getComments() {
        return comments;
    }
    
}
