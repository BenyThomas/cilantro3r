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
public class DailyRecon {

    public String txn_type;
    public String description;
    public String cbstxnsCount;
    public String cbstxnsVolume;
    public String cbsCharge;
    public String thirdPartytxnsCount;
    public String thirdPartytxnsVolume;
    public String thirdpartyCharge;
    public String difftxnCount;
    public String difftxnVolume;
    public String diffchargeVolume;
    public String first_status;
    public String second_status;
    public String initiated_by;
    public String confirmed_by;
    public String recondt;
    public String create_dt;
    public String modified_dt;

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

    public String getCbsCharge() {
        return cbsCharge;
    }

    public void setCbsCharge(String cbsCharge) {
        this.cbsCharge = cbsCharge;
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

    public String getThirdpartyCharge() {
        return thirdpartyCharge;
    }

    public void setThirdpartyCharge(String thirdpartyCharge) {
        this.thirdpartyCharge = thirdpartyCharge;
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
        return "DailyRecon{" + "txn_type=" + txn_type + ", description=" + description + ", cbstxnsCount=" + cbstxnsCount + ", cbstxnsVolume=" + cbstxnsVolume + ", cbsCharge=" + cbsCharge + ", thirdPartytxnsCount=" + thirdPartytxnsCount + ", thirdPartytxnsVolume=" + thirdPartytxnsVolume + ", thirdpartyCharge=" + thirdpartyCharge + ", difftxnCount=" + difftxnCount + ", difftxnVolume=" + difftxnVolume + ", diffchargeVolume=" + diffchargeVolume + ", first_status=" + first_status + ", second_status=" + second_status + ", initiated_by=" + initiated_by + ", confirmed_by=" + confirmed_by + ", recondt=" + recondt + ", create_dt=" + create_dt + ", modified_dt=" + modified_dt + '}';
    }

}
