package com.models;

public class Customer {

    private String firstName;
    private String middleName;
    private String lastName;
    private String responseCode;
    private String fullName;
    private String rimNumber;
    private String username;

    private String customerId;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
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

    public String getRimNumber() {
        return rimNumber;
    }

    public void setRimNumber(String rimNumber) {
        this.rimNumber = rimNumber;
    }

    public void setUsername(String username){
        this.username=username;
    }

    public String getUsername() {
        return username;
    }

    public String getResponseCode() {

        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    @Override
    public String toString() {
        return "Customer{" +
                "firstName='" + firstName + '\'' +
                ", middleName='" + middleName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", responseCode='" + responseCode + '\'' +
                ", fullName='" + fullName + '\'' +
                ", rimNumber='" + rimNumber + '\'' +
                ", username='" + username + '\'' +
                ", customerId='" + customerId + '\'' +
                '}';
    }
}
