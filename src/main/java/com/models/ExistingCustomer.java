package com.models;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PUBLIC)
@ToString
public class ExistingCustomer {
    private String accountProduct;
    private String customerNumber;
    private String accountTitle;
    private String accountNo;
    private String currency;
    private String monthlyIncome;
    private String status;
    private String branch;
    private String branchCode;
    private String gender;
    private String idType;
    private String identity;
    private String dateOfBirth;
    private String mobileNumber;
    private String photo;
    private String signature;
}
