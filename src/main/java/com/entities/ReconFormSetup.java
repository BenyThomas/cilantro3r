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
public class ReconFormSetup {

    @NotBlank(message = "Please Enter Txntype")
    private String txnType;
    @NotBlank(message = "Please Enter ttype ")
    private String ttype;

    public String getTxnType() {
        return txnType;
    }

    public void setTxnType(String txnType) {
        this.txnType = txnType;
    }

    public String getTtype() {
        return ttype;
    }

    public void setTtype(String ttype) {
        this.ttype = ttype;
    }

    @Override
    public String toString() {
        return "ReconFormSetup{" + "txnType=" + txnType + ", ttype=" + ttype + '}';
    }
    
}
