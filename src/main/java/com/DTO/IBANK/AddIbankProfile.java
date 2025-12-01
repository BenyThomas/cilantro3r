/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.IBANK;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

/**
 *
 * @author melleji.mollel
 */
public class AddIbankProfile {

    @NotBlank(message = "Please Enter Account Number")
    public String accountNo;
    @NotBlank(message = "Please enter Customer Name")
    public String customerName;
    @NotBlank(message = "Customer Id is required")
    public String customerId;
    @NotBlank(message = "Customer RIM is required")
    public String customerRim;
    @NotBlank(message = "Customer Short Name is required")
    public String custShortName;
    @NotBlank(message = "Customer Category is required")
    public String custCategory;
    @NotBlank(message = "Address1 is Required")
    public String address1;
    @NotBlank(message = "Address2 is Required")
    public String address2;
    @NotBlank(message = "Address3 is Required")
    public String address3;
//    @NotBlank(message = "Address4 is Required")
    public String address4;
    @NotBlank(message = "City is Required")
    public String city;
    @NotBlank(message = "State is Required")
    public String state;
    @NotBlank(message = "Email is Required")
    public String email;
    @NotBlank(message = "Branch Id is Required")
    public String branchId;
    @NotBlank(message = "Branch  is Required")
    public String recruitingBrach;
//    @NotBlank(message = "pfNo  is Required")
    public String pfNo;
//    @NotBlank(message = "Received date  is Required")
    public String receivedDate;
//    @NotBlank(message = "Comments is Required")
    public String comments;
    @NotBlank(message = "Mandate is Required")
    public String mandate;
    public boolean isIbankService;
    public boolean isMobileSerivce;

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerRim() {
        return customerRim;
    }

    public void setCustomerRim(String customerRim) {
        this.customerRim = customerRim;
    }

    public String getCustShortName() {
        return custShortName;
    }

    public void setCustShortName(String custShortName) {
        this.custShortName = custShortName;
    }

    public String getCustCategory() {
        return custCategory;
    }

    public void setCustCategory(String custCategory) {
        this.custCategory = custCategory;
    }

    public String getAddress1() {
        return address1;
    }

    public void setAddress1(String address1) {
        this.address1 = address1;
    }

    public String getAddress2() {
        return address2;
    }

    public void setAddress2(String address2) {
        this.address2 = address2;
    }

    public String getAddress3() {
        return address3;
    }

    public void setAddress3(String address3) {
        this.address3 = address3;
    }

    public String getAddress4() {
        return address4;
    }

    public void setAddress4(String address4) {
        this.address4 = address4;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBranchId() {
        return branchId;
    }

    public void setBranchId(String branchId) {
        this.branchId = branchId;
    }

    public String getRecruitingBrach() {
        return recruitingBrach;
    }

    public void setRecruitingBrach(String recruitingBrach) {
        this.recruitingBrach = recruitingBrach;
    }

    public String getPfNo() {
        return pfNo;
    }

    public void setPfNo(String pfNo) {
        this.pfNo = pfNo;
    }

    public String getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(String receivedDate) {
        this.receivedDate = receivedDate;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getMandate() {
        return mandate;
    }

    public void setMandate(String mandate) {
        this.mandate = mandate;
    }

    public boolean isIsIbankService() {
        return isIbankService;
    }

    public void setIsIbankService(boolean isIbankService) {
        this.isIbankService = isIbankService;
    }

    public boolean isIsMobileSerivce() {
        return isMobileSerivce;
    }

    public void setIsMobileSerivce(boolean isMobileSerivce) {
        this.isMobileSerivce = isMobileSerivce;
    }

    @Override
    public String toString() {
        return "AddIbankProfile{" + "accountNo=" + accountNo + ", customerName=" + customerName + ", customerId=" + customerId + ", customerRim=" + customerRim + ", custShortName=" + custShortName + ", custCategory=" + custCategory + ", address1=" + address1 + ", address2=" + address2 + ", address3=" + address3 + ", address4=" + address4 + ", city=" + city + ", state=" + state + ", email=" + email + ", branchId=" + branchId + ", recruitingBrach=" + recruitingBrach + ", pfNo=" + pfNo + ", receivedDate=" + receivedDate + ", comments=" + comments + ", mandate=" + mandate + ", isIbankService=" + isIbankService + ", isMobileSerivce=" + isMobileSerivce + '}';
    }
    

}
