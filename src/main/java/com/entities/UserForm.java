/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.entities;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 *
 * @author MELLEJI
 */
public class UserForm {

    @NotBlank(message = "Please Enter First Name")
    private String firstName;
    @NotBlank(message = "Please Enter Middle Name")
    private String middleName;
    @NotBlank(message = "Please Enter Last Name")
    private String lastName;
    @NotBlank(message = "Please Enter Username")
    private String username;
    @NotBlank(message = "Please enter a valid Email should be valid and it Should not be blank")
    @Email(message = "Please enter a valid Email Address")
    private String email;
    @Size(max = 12, message = "Phone Number Length must either start with 2557XXX")
    private String phone;
    @NotBlank(message = "Please Select Role")
    private String role;
    @NotBlank(message = "Please Select Branch")
    private String branchCode;
    private String trackingId;
    @NotBlank(message = "Please Select Status")
    private String userStatus;

    public String getTrackingId() {
        return trackingId;
    }

    public void setTrackingId(String trackingId) {
        this.trackingId = trackingId;
    }

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public void setBranchCode(String branchCode) {
        this.branchCode = branchCode;
    }

    public String getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(String userStatus) {
        this.userStatus = userStatus;
    }

    @Override
    public String toString() {
        return "UserForm{" + "firstName=" + firstName + ", middleName=" + middleName + ", lastName=" + lastName + ", username=" + username + ", email=" + email + ", phone=" + phone + ", role=" + role + ", branchCode=" + branchCode + ", trackingId=" + trackingId + ", userStatus=" + userStatus + '}';
    }

  

}
