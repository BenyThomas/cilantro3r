/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.Reports;

/**
 *
 * @author melleji.mollel
 */
public class AuditTrails {

    public static String username;
    public static String logdate;
    public static String roleid;
    public static String ipaddress;
    public static String functionName;
    public static String Status;
    public static String comments;

    public static String getLogdate() {
        return logdate;
    }

    public static void setLogdate(String logdate) {
        AuditTrails.logdate = logdate;
    }


    public static String getRoleid() {
        return roleid;
    }

    public static void setRoleid(String roleid) {
        AuditTrails.roleid = roleid;
    }

    public static String getIpaddress() {
        return ipaddress;
    }

    public static void setIpaddress(String ipaddress) {
        AuditTrails.ipaddress = ipaddress;
    }

    public static String getFunctionName() {
        return functionName;
    }

    public static void setFunctionName(String functionName) {
        AuditTrails.functionName = functionName;
    }

    public static String getStatus() {
        return Status;
    }

    public static void setStatus(String Status) {
        AuditTrails.Status = Status;
    }

    public static String getComments() {
        return comments;
    }

    public static void setComments(String comments) {
        AuditTrails.comments = comments;
    }

    public static String getUsername() {
        return username;
    }

    public static void setUsername(String username) {
        AuditTrails.username = username;
    }
   
    

}
