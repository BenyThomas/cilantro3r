/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.entities;

import javax.validation.constraints.NotBlank;

/**
 *
 * @author MELLEJI
 */
public class ReconForm {

    @NotBlank(message = "Please select Transaction type")
    private String txnType;
    @NotBlank(message = "Please select Transaction Date!!!!")
    private String txnDate;

    public String getTxnType() {
        return txnType;
    }

    public void setTxnType(String txnType) {
        this.txnType = txnType;
    }

    public String getTxnDate() {
        return txnDate;
    }

    public void setTxnDate(String txnDate) {
        this.txnDate = txnDate;
    }

    @Override
    public String toString() {
        return "reconForm{" + "txnType=" + txnType + ", txnDate=" + txnDate + '}';
    }
    
}
