/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.pension;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author melleji.mollel
 */
public class PsssfBatchRequest {

    private String batchId;
    private String batchno;
    private String batchDate;
    private String institution;
    private String batchDescription;
    private String totalAmount;
    private String userid;
    private String NoOfPensioners;
    List<PsssfBatchBeneficiary> beneficiaries = new ArrayList<>();

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getBatchDate() {
        return batchDate;
    }

    public void setBatchDate(String batchDate) {
        this.batchDate = batchDate;
    }

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public String getBatchDescription() {
        return batchDescription;
    }

    public void setBatchDescription(String batchDescription) {
        this.batchDescription = batchDescription;
    }

    public String getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(String totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public List<PsssfBatchBeneficiary> getBeneficiaries() {
        return beneficiaries;
    }

    public void setBeneficiaries(List<PsssfBatchBeneficiary> beneficiaries) {
        this.beneficiaries = beneficiaries;
    }

    public String getNoOfPensioners() {
        return NoOfPensioners;
    }

    public void setNoOfPensioners(String NoOfPensioners) {
        this.NoOfPensioners = NoOfPensioners;
    }

    @Override
    public String toString() {
        return "PsssfBatchRequest{" + "batchId=" + batchId + ", batchDate=" + batchDate + ", institution=" + institution + ", batchDescription=" + batchDescription + ", totalAmount=" + totalAmount + ", userid=" + userid + ", NoOfPensioners=" + NoOfPensioners + ", beneficiaries=" + beneficiaries + '}';
    }

    public String getBatchno() {
        return batchno;
    }

    public void setBatchno(String batchno) {
        this.batchno = batchno;
    }
}
