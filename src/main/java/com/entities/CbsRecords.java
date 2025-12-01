/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.entities;

/**
 *
 * @author MELLEJI
 */
public class CbsRecords {

    private String lastPtid;
    private String transref;
    private String amount;
    private String txnDate;
    private String valueDate;
    private String contraAcct;
    private String txnid;
    private String description;
    private String dr_cr_ind;
    private String postBalance;
    private String sourceAccount;

    public String getLastPtid() {
        return lastPtid;
    }

    public void setLastPtid(String lastPtid) {
        this.lastPtid = lastPtid;
    }

    public String getTransref() {
        return transref;
    }

    public void setTransref(String transref) {
        this.transref = transref;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getTxnDate() {
        return txnDate;
    }

    public void setTxnDate(String txnDate) {
        this.txnDate = txnDate;
    }

    public String getValueDate() {
        return valueDate;
    }

    public void setValueDate(String valueDate) {
        this.valueDate = valueDate;
    }

    public String getContraAcct() {
        return contraAcct;
    }

    public void setContraAcct(String contraAcct) {
        this.contraAcct = contraAcct;
    }

    public String getTxnid() {
        return txnid;
    }

    public void setTxnid(String txnid) {
        this.txnid = txnid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDr_cr_ind() {
        return dr_cr_ind;
    }

    public void setDr_cr_ind(String dr_cr_ind) {
        this.dr_cr_ind = dr_cr_ind;
    }

    public String getPostBalance() {
        return postBalance;
    }

    public void setPostBalance(String postBalance) {
        this.postBalance = postBalance;
    }

    public String getSourceAccount() {
        return sourceAccount;
    }

    public void setSourceAccount(String sourceAccount) {
        this.sourceAccount = sourceAccount;
    }

    @Override
    public String toString() {
        return "cbsRecords{" + "lastPtid=" + lastPtid + ", transref=" + transref + ", amount=" + amount + ", txnDate=" + txnDate + ", valueDate=" + valueDate + ", contraAcct=" + contraAcct + ", txnid=" + txnid + ", description=" + description + ", dr_cr_ind=" + dr_cr_ind + ", postBalance=" + postBalance + ", sourceAccount=" + sourceAccount + '}';
    }

}
