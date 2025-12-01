package com.entities;

public class OutstandingLoan {
    private String accountNo;
    private String custRim;
    private String fullName;
    private String dob;
    private String branchId;
    private String branchCode;
    private String branchName;
    private String phoneNo;
    private Integer outstandingPrincipal;
    private Double outstandingFee;
    private Double totalOutstandingLoan;
    public String getAccountNo() {
        return accountNo;
    }
    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }
    public String getCustRim() {
        return custRim;
    }
    public void setCustRim(String custRim) {
        this.custRim = custRim;
    }
    public String getFullName() {
        return fullName;
    }
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    public String getDob() {
        return dob;
    }
    public void setDob(String dob) {
        this.dob = dob;
    }
    public String getBranchId() {
        return branchId;
    }
    public void setBranchId(String branchId) {
        this.branchId = branchId;
    }
    public String getBranchCode() {
        return branchCode;
    }
    public void setBranchCode(String branchCode) {
        this.branchCode = branchCode;
    }
    public String getBranchName() {
        return branchName;
    }
    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }
    public String getPhoneNo() {
        return phoneNo;
    }
    public void setPhoneNo(String phoneNo) {
        this.phoneNo = phoneNo;
    }
    public Integer getOutstandingPrincipal() {
        return outstandingPrincipal;
    }
    public void setOutstandingPrincipal(Integer outstandingPrincipal) {
        this.outstandingPrincipal = outstandingPrincipal;
    }
    public Double getOutstandingFee() {
        return outstandingFee;
    }
    public void setOutstandingFee(Double outstandingFee) {
        this.outstandingFee = outstandingFee;
    }
    public Double getTotalOutstandingLoan() {
        return totalOutstandingLoan;
    }
    public void setTotalOutstandingLoan(Double totalOutstandingLoan) {
        this.totalOutstandingLoan = totalOutstandingLoan;
    }
}