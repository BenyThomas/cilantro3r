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
 * @author melleji.mollel
 */
public class AddIbankSignatories {
    @NotBlank(message = "Full Name is Required")
    public String fullName;
    @NotBlank(message = "Username is required")
    public String username;
    @NotBlank(message = "Customer ID is required")
    public String customerId;
    @Pattern(regexp="(^255[1-9]{2}[0-9]{7})",message = "must start with 255. ")
    @NotBlank(message = "Phone number is required for Login")
    public String phoneNumber;
    @NotBlank(message = "Email Address is Required")
    public String email;
    public String transferAccess;
    @NotBlank(message = "Role is Required")
    public String role;
    @NotBlank(message = "View Access is Required")
    public String viewAccess;
    @NotBlank(message = "Account Limit is required.")
    public String accountLimit;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber=phoneNumber;
//        if (phoneNumber != null) {
//            String trimNumber = phoneNumber.replaceAll("\\s", "");
//            String number = "255" + trimNumber.substring(trimNumber.length() - 9);
//            this.phoneNumber = number;
//        }
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTransferAccess() {
        return transferAccess;
    }

    public void setTransferAccess(String transferAccess) {
        this.transferAccess = transferAccess;
    }

    public String getViewAccess() {
        return viewAccess;
    }

    public void setViewAccess(String viewAccess) {
        this.viewAccess = viewAccess;
    }

    public String getAccountLimit() {
        return accountLimit;
    }

    public void setAccountLimit(String accountLimit) {
        this.accountLimit = accountLimit;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return "AddIbankSignatories{" +
                ", fullName='" + fullName + '\'' +
                ", username='" + username + '\'' +
                ", customerId='" + customerId + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", email='" + email + '\'' +
                ", transferAccess='" + transferAccess + '\'' +
                ", role='" + role + '\'' +
                ", viewAccess='" + viewAccess + '\'' +
                ", accountLimit='" + accountLimit + '\'' +
                '}';
    }
}
