package com.DTO.Teller;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
@ToString
public class FinanceMultipleGlPosting {
//    @NotBlank(message = "Please Enter  GL account (source of Fund)")
//    public String senderAccount;

    @NotBlank(message = "Please Enter Sender Name")
    public String senderName;

//    @NotBlank(message = "Please Enter Amount (With/Without VAT) i.e Total amount to Service Provider")
//    public String amount;

    @NotBlank(message = "Please Select paying currency")
    public String currency;

    @NotBlank(message = "Please Enter Beneficiary account number")
    public String beneficiaryAccount;

    @NotBlank(message = "Please Enter Beneficiary Name")
    public String beneficiaryName;

    @NotBlank(message = "Please Select Beneficiary Bank")
    public String beneficiaryBIC;

    @NotBlank(message = "Please Enter Sender Address")
    public String senderAddress;

    @NotBlank(message = "Please Enter Beneficiary Contact")
    public String beneficiaryContact;

    @NotBlank(message = "Please Enter Sender Phone")
    public String senderPhone;

    @NotBlank(message = "Please Enter Payment purpose")
    public String description;

    @NotBlank(message = "Please Enter amount that can be used to compute with Holding tax")
    public String taxableAmount;

    @NotBlank(message = "Please Select  Tax Category")
    public String taxRate;

    public String intermediaryBank;

}
