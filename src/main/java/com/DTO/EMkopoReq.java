package com.DTO;

import lombok.Data;

@Data
public class EMkopoReq {
    private String reference;
    private String sourceAccount;
    private String branchCode;
    private String destinationAccount;
    private String amount;
    private String currency;
    private String beneficiaryName;
    private String beneficiaryBIC;
    private String beneficiaryContact;
    private String senderBIC;
    private String senderPhone;
    private String senderAddress;
    private String senderName;
    private String purpose;
    private String initiatorId;
    private String callBackUrl;
}
