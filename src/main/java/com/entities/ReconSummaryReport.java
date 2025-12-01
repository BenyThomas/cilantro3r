/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.entities;

import java.util.List;
import java.util.Map;

/**
 *
 * @author MELLEJI
 */
public class ReconSummaryReport {

    String txnType;

    List<Map<String, Object>> cbsTxns;
    List<Map<String, Object>> notInCbsTxns;
    List<Map<String, Object>> CbsSuccessThirdPartyFailedTxns;
    List<Map<String, Object>> thirdPartTxns;
    List<Map<String, Object>> notInthirdPartTxns;
    List<Map<String, Object>> thirdPartySuccessCBSFailedTxns;
    List<Map<String, Object>> diffTxns;

    public List<Map<String, Object>> getNotInCbsTxns() {
        return notInCbsTxns;
    }

    public void setNotInCbsTxns(List<Map<String, Object>> notInCbsTxns) {
        this.notInCbsTxns = notInCbsTxns;
    }

    public List<Map<String, Object>> getCbsSuccessThirdPartyFailedTxns() {
        return CbsSuccessThirdPartyFailedTxns;
    }

    public void setCbsSuccessThirdPartyFailedTxns(List<Map<String, Object>> CbsSuccessThirdPartyFailedTxns) {
        this.CbsSuccessThirdPartyFailedTxns = CbsSuccessThirdPartyFailedTxns;
    }

    public List<Map<String, Object>> getNotInthirdPartTxns() {
        return notInthirdPartTxns;
    }

    public void setNotInthirdPartTxns(List<Map<String, Object>> notInthirdPartTxns) {
        this.notInthirdPartTxns = notInthirdPartTxns;
    }

    public String getTxnType() {
        return txnType;
    }

    public void setTxnType(String txnType) {
        this.txnType = txnType;
    }

    public List<Map<String, Object>> getCbsTxns() {
        return cbsTxns;
    }

    public void setCbsTxns(List<Map<String, Object>> cbsTxns) {
        this.cbsTxns = cbsTxns;
    }

    public List<Map<String, Object>> getThirdPartTxns() {
        return thirdPartTxns;
    }

    public void setThirdPartTxns(List<Map<String, Object>> thirdPartTxns) {
        this.thirdPartTxns = thirdPartTxns;
    }

    public List<Map<String, Object>> getDiffTxns() {
        return diffTxns;
    }

    public void setDiffTxns(List<Map<String, Object>> diffTxns) {
        this.diffTxns = diffTxns;
    }

    public List<Map<String, Object>> getThirdPartySuccessCBSFailedTxns() {
        return thirdPartySuccessCBSFailedTxns;
    }

    public void setThirdPartySuccessCBSFailedTxns(List<Map<String, Object>> thirdPartySuccessCBSFailedTxns) {
        this.thirdPartySuccessCBSFailedTxns = thirdPartySuccessCBSFailedTxns;
    }

    @Override
    public String toString() {
        return "ReconSummaryReport{" + "txnType=" + txnType + ", cbsTxns=" + cbsTxns + ", notInCbsTxns=" + notInCbsTxns + ", CbsSuccessThirdPartyFailedTxns=" + CbsSuccessThirdPartyFailedTxns + ", thirdPartTxns=" + thirdPartTxns + ", notInthirdPartTxns=" + notInthirdPartTxns + ", thirdPartySuccessCBSFailedTxns=" + thirdPartySuccessCBSFailedTxns + ", diffTxns=" + diffTxns + '}';
    }

}
