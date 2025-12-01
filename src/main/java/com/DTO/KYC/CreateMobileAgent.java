/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.KYC;

import jakarta.validation.constraints.NotBlank;

/**
 *
 * @author melleji.mollel
 */
public class CreateMobileAgent {

    @NotBlank(message = "Please enter first name")
    public String firstName;
    @NotBlank(message = "Please enter middle name")
    public String middleName;
    @NotBlank(message = "Please enter Last Name")
    public String lastName;
    @NotBlank(message = "Please enter username")
    public String username;
    @NotBlank(message = "Please enter phone number")
    public String phoneNumber;
    public String deviceId;
    @NotBlank(message = "Please enter branch code")
    public String branchCode;
    @NotBlank(message = "Please enter email")
    public String email;
    public String createdBy;
    public String createdDate;
    public boolean isAgent;
    public String account;
    public String category;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public void setBranchCode(String branchCode) {
        this.branchCode = branchCode;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public boolean isAgent(){
        return isAgent;
    }

    public void setAgent(boolean isAgent){
        this.isAgent = isAgent;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public String toString() {
        return "CreateMobileAgent{" + "firstName=" + firstName + ", middleName=" + middleName + ", lastName=" +
                lastName + ", username=" + username + ", phoneNumber=" + phoneNumber + ", deviceId=" + deviceId +
                ", branchCode=" + branchCode + ", email=" + email + ", createdBy=" + createdBy + ", createdDate=" +
                createdDate + ", account=" + account + ", category=" + category + "}";
    }

}
