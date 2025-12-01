package com.DTO.psssf;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
@ToString
public class PensionVerificationForm {
    @NotBlank(message = "Please Enter Loan Amount")
    private String loanAmount;
    @NotBlank(message = "Please Enter Monthly Installment")
    private String monthlyInst;
    @NotBlank(message = "Please Enter Period")
    private String period;
    @NotBlank(message = "Please Enter Narrations")
    private String narration;
    @NotBlank(message = "Please Choose Bank Type")
    private String bankType;
    @NotBlank(message = "Please Enter Account Number Amount")
    private String accNumber;
    @NotBlank(message = "Please Select Branch")
    private String branchCode;
    @NotBlank(message = "Please click Query Details")
    private String rubikonActNM;
    private String pensionerName;
//    @JsonProperty("submittedBy")
//    private String submittedBy;
//    @JsonProperty("pensionerID")
//    private String pensionerID;
//    @JsonProperty("clearanceDoc")
//    private String clearanceDoc;
//    @JsonProperty("changeBankAccDoc")
//    private String changeBankAccDoc;
//    @JsonProperty("callBackUrl")
//    private String callBackUrl;
//    @Override
//    public String toString() {
//        Gson gson = new GsonBuilder().create();
//        return gson.toJson(this);
//    }
}
