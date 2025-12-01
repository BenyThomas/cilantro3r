/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.entities;

import jakarta.validation.constraints.NotBlank;

/**
 *
 * @author MELLEJI
 */
public class ReportsForm {

    @NotBlank(message = "Please select Transaction type")
    private String txnType;
    @NotBlank(message = "Please select Transaction From Date!!!!")
    private String fromdate;
    @NotBlank(message = "Please select Transaction To Date!!!!")
    private String todate;

    
    public String getTxnType() {
        return txnType;
    }

    public void setTxnType(String txnType) {
        this.txnType = txnType;
    }

    public String getFromdate() {
        return fromdate;
    }

    public void setFromdate(String fromdate) {
        this.fromdate = fromdate;
    }

    public String getTodate() {
        return todate;
    }

    public void setTodate(String todate) {
        this.todate = todate;
    }

    @Override
    public String toString() {
        return "ReportsForm{" + "txnType=" + txnType + ", fromdate=" + fromdate + ", todate=" + todate + '}';
    }

}