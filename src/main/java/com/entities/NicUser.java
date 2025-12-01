package com.entities;

import java.io.Serializable;
import java.util.Date;


public class NicUser implements Serializable {

    private String id;

    private String createdBy;


    private String updatedBy;

    private Date createdOn;

    private Date updatedOn;

    private String quoteNumber;

    private String productId;

    private String sumAssured;

    private String policyTerm;

    private String commencementDate;

    private String paymentFrequency;

    private String amountToSave;

    private String firstName;

    private String middleName;

    private String lastName;

    private String identityNumber;

    private String email;

    private String gender;

    private String msisdn;

    private String dateOfBirth;

    private String occupation;

    private boolean code;

    private String message;

    private String premium;

    private String proposalId;

    private String paymentDate;

    private String controlNumber;

    private String policyNumber;

    private String receiptNumber;

    private String policyStartDate;

    private String policyEndDate;

    private String policyDocument;

    private String language;

    private String status;

    private byte[] signature;

    private String firebaseToken;

    @Override
    public String toString() {
        return "NICUser(id=" + id + ", firstName=" + firstName + ", middleName=" + middleName + ", lastName=" + lastName + ", msisdn='" + msisdn + "', identityNumber='" + identityNumber + "', gender=" + gender + ", dateOfBirth=" + dateOfBirth + ", productId=" + productId + ", quoteNumber=" + quoteNumber + ", sumAssured='" + sumAssured + "', language=" + language + ", policyTerm=" + policyTerm + ", status=" + status + ", commencementDate=" + commencementDate + ", paymentFrequency=" + paymentFrequency + ", amount=" + amountToSave + ", occupation=" + occupation + ", premium=" + premium + ", proposalId=" + proposalId + ", paymentDate=" + paymentDate + ", policyNumber=" + policyNumber + ", policyStartDate=" + policyStartDate + ", policyEndDate=" + policyEndDate + ", firebaseToken=" + firebaseToken + ", receiptNumber=" + receiptNumber + ", policyDocument=" + policyDocument + ")";
    }
}

