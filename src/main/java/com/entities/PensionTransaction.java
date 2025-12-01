package com.entities;

import lombok.*;

@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PUBLIC)
@NoArgsConstructor
@ToString
public class PensionTransaction {
    private String id;
    private String trackingNo;
    private String name;
    private String cbsName;
    private int percentageMatch;
    private String currency;
    private double amount;
    private String account;
    private String channelIdentifier;
    private String bankCode;
    private String pensionerId;
    private String batchReference;
    private String bankReference;
    private String description;
    private String createdBy;
    private java.util.Date createDt;
    private String status;
    private String responseCode;
    private String message;
    private String comments;
    private String cbsStatus;
    private String verifiedBy;
    private java.util.Date verifiedDt;
    private String approvedBy;
    private java.util.Date approvedDt;
    private int payrollMonth;
    private int payrollYear;
    private String branchId;
    private String drAccount;
    private String module;
    private String reverse;
    private int tries;
    private String timestamp;
}

