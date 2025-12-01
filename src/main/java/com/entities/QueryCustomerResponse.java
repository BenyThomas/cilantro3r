package com.entities;

import lombok.*;

@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PUBLIC)
@NoArgsConstructor
@ToString
public class QueryCustomerResponse {
    private String result;
    private String reference;
    private String message;
    private String custName;
    private String custNo;
    private String custCat;
    private String custId;
    private String photo;
    private String signature;
    private String accountName;
    private String accountNumber;
    private String accountType;
    private String acctId;
    private String mobileNumber;
    private String status;
    private String buCode;
    private String buId;
    private String buName;
    private String glPrefix;
    private String code;
    private String id;
    private String name;
    private String productId;
    private String dob;
    private String gender;
}
