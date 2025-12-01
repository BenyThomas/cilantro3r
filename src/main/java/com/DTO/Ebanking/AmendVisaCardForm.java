package com.DTO.Ebanking;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.intellij.lang.annotations.RegExp;

import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
@ToString
public class AmendVisaCardForm {
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
//    @NotBlank(message = "Please Enter City")
//    public String city;
//    @NotBlank(message = "Please Enter State")
//    public String state;
    @RegExp(prefix = "^255")
    @NotBlank(message = "Please Enter Phone And Start With 255")
    public String phoneNumber;
//    @NotBlank(message = "Please Enter domicile branch")
//    public String branchId;
    @NotBlank(message = "Please Enter branch to collect Card")
    public String recruitingBrach;
    public String reference;
    public String originatingBranch;
    public String createdBy;
    @NotBlank(message = "Please Enter Valid Email")
    public String customerEmail;
}
