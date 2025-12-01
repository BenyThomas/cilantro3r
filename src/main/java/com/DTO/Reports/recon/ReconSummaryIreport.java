/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.Reports.recon;

/**
 *
 * @author melleji.mollel
 */
public class ReconSummaryIreport {

    public String txn_type;
    public String description;
    public String cbsopeningbalance;
    public String cbstxnsCount;
    public String cbstxnsVolume;
    public String cbsClosingBalance;
    public String cbsCharge;
    public String thirdpartyOpeningBalance;
    public String thirdPartytxnsCount;
    public String thirdPartytxnsVolume;
    public String thirdpartyClosingBalance;
    public String thirdpartyCharge;
    public String diffOpeningBalance;
    public String difftxnCount;
    public String difftxnVolume;
    public String diffchargeVolume;
    public String diffCloasingBalance;
    public String first_status;
    public String second_status;
    public String initiated_by;
    public String recon_initiator;
    public String confirmed_by;
    public String recondt;
    public String create_dt;
    public String modified_dt;

    public ReconSummaryIreport(String txn_type, String description, String cbsopeningbalance, String cbstxnsCount, String cbstxnsVolume, String cbsClosingBalance, String cbsCharge, String thirdpartyOpeningBalance, String thirdPartytxnsCount, String thirdPartytxnsVolume, String thirdpartyClosingBalance, String thirdpartyCharge, String diffOpeningBalance, String difftxnCount, String difftxnVolume, String diffchargeVolume, String diffCloasingBalance, String first_status, String second_status, String initiated_by, String recon_initiator, String confirmed_by, String recondt, String create_dt, String modified_dt) {
        this.txn_type = txn_type;
        this.description = description;
        this.cbsopeningbalance = cbsopeningbalance;
        this.cbstxnsCount = cbstxnsCount;
        this.cbstxnsVolume = cbstxnsVolume;
        this.cbsClosingBalance = cbsClosingBalance;
        this.cbsCharge = cbsCharge;
        this.thirdpartyOpeningBalance = thirdpartyOpeningBalance;
        this.thirdPartytxnsCount = thirdPartytxnsCount;
        this.thirdPartytxnsVolume = thirdPartytxnsVolume;
        this.thirdpartyClosingBalance = thirdpartyClosingBalance;
        this.thirdpartyCharge = thirdpartyCharge;
        this.diffOpeningBalance = diffOpeningBalance;
        this.difftxnCount = difftxnCount;
        this.difftxnVolume = difftxnVolume;
        this.diffchargeVolume = diffchargeVolume;
        this.diffCloasingBalance = diffCloasingBalance;
        this.first_status = first_status;
        this.second_status = second_status;
        this.initiated_by = initiated_by;
        this.recon_initiator = recon_initiator;
        this.confirmed_by = confirmed_by;
        this.recondt = recondt;
        this.create_dt = create_dt;
        this.modified_dt = modified_dt;
    }

    public String getTxn_type() {
        return txn_type;
    }

    public void setTxn_type(String txn_type) {
        this.txn_type = txn_type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCbsopeningbalance() {
        return cbsopeningbalance;
    }

    public void setCbsopeningbalance(String cbsopeningbalance) {
        this.cbsopeningbalance = cbsopeningbalance;
    }

    public String getCbstxnsCount() {
        return cbstxnsCount;
    }

    public void setCbstxnsCount(String cbstxnsCount) {
        this.cbstxnsCount = cbstxnsCount;
    }

    public String getCbstxnsVolume() {
        return cbstxnsVolume;
    }

    public void setCbstxnsVolume(String cbstxnsVolume) {
        this.cbstxnsVolume = cbstxnsVolume;
    }

    public String getCbsClosingBalance() {
        return cbsClosingBalance;
    }

    public void setCbsClosingBalance(String cbsClosingBalance) {
        this.cbsClosingBalance = cbsClosingBalance;
    }

    public String getCbsCharge() {
        return cbsCharge;
    }

    public void setCbsCharge(String cbsCharge) {
        this.cbsCharge = cbsCharge;
    }

    public String getThirdpartyOpeningBalance() {
        return thirdpartyOpeningBalance;
    }

    public void setThirdpartyOpeningBalance(String thirdpartyOpeningBalance) {
        this.thirdpartyOpeningBalance = thirdpartyOpeningBalance;
    }

    public String getThirdPartytxnsCount() {
        return thirdPartytxnsCount;
    }

    public void setThirdPartytxnsCount(String thirdPartytxnsCount) {
        this.thirdPartytxnsCount = thirdPartytxnsCount;
    }

    public String getThirdPartytxnsVolume() {
        return thirdPartytxnsVolume;
    }

    public void setThirdPartytxnsVolume(String thirdPartytxnsVolume) {
        this.thirdPartytxnsVolume = thirdPartytxnsVolume;
    }

    public String getThirdpartyClosingBalance() {
        return thirdpartyClosingBalance;
    }

    public void setThirdpartyClosingBalance(String thirdpartyClosingBalance) {
        this.thirdpartyClosingBalance = thirdpartyClosingBalance;
    }

    public String getThirdpartyCharge() {
        return thirdpartyCharge;
    }

    public void setThirdpartyCharge(String thirdpartyCharge) {
        this.thirdpartyCharge = thirdpartyCharge;
    }

    public String getDiffOpeningBalance() {
        return diffOpeningBalance;
    }

    public void setDiffOpeningBalance(String diffOpeningBalance) {
        this.diffOpeningBalance = diffOpeningBalance;
    }

    public String getDifftxnCount() {
        return difftxnCount;
    }

    public void setDifftxnCount(String difftxnCount) {
        this.difftxnCount = difftxnCount;
    }

    public String getDifftxnVolume() {
        return difftxnVolume;
    }

    public void setDifftxnVolume(String difftxnVolume) {
        this.difftxnVolume = difftxnVolume;
    }

    public String getDiffchargeVolume() {
        return diffchargeVolume;
    }

    public void setDiffchargeVolume(String diffchargeVolume) {
        this.diffchargeVolume = diffchargeVolume;
    }

    public String getDiffCloasingBalance() {
        return diffCloasingBalance;
    }

    public void setDiffCloasingBalance(String diffCloasingBalance) {
        this.diffCloasingBalance = diffCloasingBalance;
    }

    public String getFirst_status() {
        return first_status;
    }

    public void setFirst_status(String first_status) {
        this.first_status = first_status;
    }

    public String getSecond_status() {
        return second_status;
    }

    public void setSecond_status(String second_status) {
        this.second_status = second_status;
    }

    public String getInitiated_by() {
        return initiated_by;
    }

    public void setInitiated_by(String initiated_by) {
        this.initiated_by = initiated_by;
    }

    public String getRecon_initiator() {
        return recon_initiator;
    }

    public void setRecon_initiator(String recon_initiator) {
        this.recon_initiator = recon_initiator;
    }

    public String getConfirmed_by() {
        return confirmed_by;
    }

    public void setConfirmed_by(String confirmed_by) {
        this.confirmed_by = confirmed_by;
    }

    public String getRecondt() {
        return recondt;
    }

    public void setRecondt(String recondt) {
        this.recondt = recondt;
    }

    public String getCreate_dt() {
        return create_dt;
    }

    public void setCreate_dt(String create_dt) {
        this.create_dt = create_dt;
    }

    public String getModified_dt() {
        return modified_dt;
    }

    public void setModified_dt(String modified_dt) {
        this.modified_dt = modified_dt;
    }

    @Override
    public String toString() {
        return "ReconSummaryIreport{" + "txn_type=" + txn_type + ", description=" + description + ", cbsopeningbalance=" + cbsopeningbalance + ", cbstxnsCount=" + cbstxnsCount + ", cbstxnsVolume=" + cbstxnsVolume + ", cbsClosingBalance=" + cbsClosingBalance + ", cbsCharge=" + cbsCharge + ", thirdpartyOpeningBalance=" + thirdpartyOpeningBalance + ", thirdPartytxnsCount=" + thirdPartytxnsCount + ", thirdPartytxnsVolume=" + thirdPartytxnsVolume + ", thirdpartyClosingBalance=" + thirdpartyClosingBalance + ", thirdpartyCharge=" + thirdpartyCharge + ", diffOpeningBalance=" + diffOpeningBalance + ", difftxnCount=" + difftxnCount + ", difftxnVolume=" + difftxnVolume + ", diffchargeVolume=" + diffchargeVolume + ", diffCloasingBalance=" + diffCloasingBalance + ", first_status=" + first_status + ", second_status=" + second_status + ", initiated_by=" + initiated_by + ", recon_initiator=" + recon_initiator + ", confirmed_by=" + confirmed_by + ", recondt=" + recondt + ", create_dt=" + create_dt + ", modified_dt=" + modified_dt + '}';
    }

}
