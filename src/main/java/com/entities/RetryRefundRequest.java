/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.entities;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotBlank;

/**
 *
 * @author MELLEJI
 */
public class RetryRefundRequest {

    @NotBlank
    private String reason;

    public List<String> txnid;
    public List<String> sourceAccount;
    public List<String> destinationAccount;
    public List<String> amount;

    public List<String> getAll;

    public  List<List<String>> getGetAll() {
        List<List<String>> g = new ArrayList<>();
        g.add(this.txnid);
        g.add(this.sourceAccount);
        g.add(this.destinationAccount);
        g.add(this.amount);
        return g;
    }

    public void setGetAll(List<String> getAll) {
        this.getAll = getAll;
    }

    public List<String> getTxnid() {
        return txnid;
    }

    public void setTxnid(List<String> txnid) {
        this.txnid = txnid;
    }

    public List<String> getSourceAccount() {
        return sourceAccount;
    }

    public void setSourceAccount(List<String> sourceAccount) {
        this.sourceAccount = sourceAccount;
    }

    public List<String> getDestinationAccount() {
        return destinationAccount;
    }

    public void setDestinationAccount(List<String> destinationAccount) {
        this.destinationAccount = destinationAccount;
    }

    public List<String> getAmount() {
        return amount;
    }

    public void setAmount(List<String> amount) {
        this.amount = amount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public String toString() {
        return "RetryRefundRequest{" + "txnid=" + txnid + ", sourceAccount=" + sourceAccount + ", destinationAccount=" + destinationAccount + ", amount=" + amount + '}';
    }

}
