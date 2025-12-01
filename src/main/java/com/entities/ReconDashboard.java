/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.entities;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 *
 * @author MELLEJI
 */
public class ReconDashboard implements Serializable {

    List<Map<String, Object>> getCoreBanking;
    List<Map<String, Object>> getThirdParty;
    String txn_type;

    public String getTxn_type() {
        return txn_type;
    }

    public void setTxn_type(String txn_type) {
        this.txn_type = txn_type;
    }

    public List<Map<String, Object>> getGetCoreBanking() {
        return getCoreBanking;
    }

    public void setGetCoreBanking(List<Map<String, Object>> getCoreBanking) {
        this.getCoreBanking = getCoreBanking;
    }

    public List<Map<String, Object>> getGetThirdParty() {
        return getThirdParty;
    }

    public void setGetThirdParty(List<Map<String, Object>> getThirdParty) {
        this.getThirdParty = getThirdParty;
    }

    @Override
    public String toString() {
        return "ReconDashboard{" + "getCoreBanking=" + getCoreBanking + ", getThirdParty=" + getThirdParty + ", txn_type=" + txn_type + '}';
    }

    
}
