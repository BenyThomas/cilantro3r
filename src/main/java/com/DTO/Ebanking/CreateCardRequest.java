/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.Ebanking;

import lombok.ToString;
import org.hibernate.validator.constraints.Email;
import org.intellij.lang.annotations.RegExp;

import javax.validation.constraints.NotBlank;

/**
 *
 * @author melleji.mollel
 */
@ToString
public class CreateCardRequest {

    @NotBlank(message = "Please Enter Account Number")
    public String accountNo;
    @NotBlank(message = "Please Enter Customer Name")
    public String customerName;
    @NotBlank(message = "Please Enter Customer Id")
    public String customerId;
    @NotBlank(message = "Please Enter Customer Rim No")
    public String customerRim;
    @NotBlank(message = "Please Enter Customer Short Name")
    public String custShortName;
    @NotBlank(message = "Please Enter Customer Category")
    public String custCategory;
    @NotBlank(message = "Please Enter Address1")
    public String address1;
    @NotBlank(message = "Please Enter Address2")
    public String address2;
    @NotBlank(message = "Please Enter Address3")
    public String address3;
    @NotBlank(message = "Please Enter Address4")
    public String address4;
    @NotBlank(message = "Please Enter City")
    public String city;
    @NotBlank(message = "Please Enter State")
    public String state;
    @NotBlank(message = "Please Enter phone number")
    public String phoneNumber;
    @NotBlank(message = "Please select country code")
    public String countryCode;
    @NotBlank(message = "Please Enter domicile branch")
    public String branchId;
    @NotBlank(message = "Please Enter branch to collect Card")
    public String recruitingBrach;
    @NotBlank(message = "Card type is required")
    public String cardType;
    public String reference;
    public String originatingBranch;
    public String createdBy;
    //@NotBlank(message = "Please Enter Valid Email")
    public String customerEmail;

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

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
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

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getOriginatingBranch() {
        return originatingBranch;
    }

    public void setOriginatingBranch(String originatingBranch) {
        this.originatingBranch = originatingBranch;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

}
