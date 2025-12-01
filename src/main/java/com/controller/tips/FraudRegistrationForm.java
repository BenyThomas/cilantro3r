package com.controller.tips;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotEmpty;


public class FraudRegistrationForm {
    @NotEmpty(message = "Fsp is required")
    private String fsp;
    @NotEmpty(message = "Name is required")
    private String fullName;
    @NotEmpty(message = "Identity type is required")
    private String identityType;
    @NotEmpty(message = "Identity value is required")
    private String identityValue;
    @NotEmpty(message = "Identifier type is required")
    private  String identifierType;
    @NotEmpty(message = "Identifier value is required")
    private String identifierValue;
    @NotEmpty(message = "reason is required")
    private String reasons;

    public String getFsp() {
        return fsp;
    }

    public void setFsp(String fsp) {
        this.fsp = fsp;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getIdentityType() {
        return identityType;
    }

    public void setIdentityType(String identityType) {
        this.identityType = identityType;
    }

    public String getIdentityValue() {
        return identityValue;
    }

    public void setIdentityValue(String identityValue) {
        this.identityValue = identityValue;
    }

    public String getIdentifierType() {
        return identifierType;
    }

    public void setIdentifierType(String identifierType) {
        this.identifierType = identifierType;
    }

    public String getIdentifierValue() {
        return identifierValue;
    }

    public void setIdentifierValue(String identifierValue) {
        this.identifierValue = identifierValue;
    }

    public String getReasons() {
        return reasons;
    }

    public void setReasons(String reasons) {
        this.reasons = reasons;
    }

    @Override
    public String toString() {
        return "FraudRegistrationForm{" +
                "fsp='" + fsp + '\'' +
                ", fullName='" + fullName + '\'' +
                ", identityType='" + identityType + '\'' +
                ", identityValue='" + identityValue + '\'' +
                ", identifierType='" + identifierType + '\'' +
                ", identifierValue='" + identifierValue + '\'' +
                ", reasons='" + reasons + '\'' +
                '}';
    }
}
