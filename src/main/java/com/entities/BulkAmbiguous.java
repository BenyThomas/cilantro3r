/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.entities;

/**
 *
 * @author HP
 */

public class BulkAmbiguous {

    public String txndate;
    public String txnReference;

    public String getTxndate() {
        return txndate;
    }

    public void setTxndate(String txndate) {
        this.txndate = txndate;
    }

    public String getTxnReference() {
        return txnReference;
    }

    public void setTxnReference(String txnReference) {
        this.txnReference = txnReference;
    }

    @Override
    public String toString() {
        return "BulkAmbiguous{" + "txndate=" + txndate + ", txnReference=" + txnReference + '}';
    }
    
    
}
