package com.helper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransferItem {
    private String createDate;
    private String employeeId;
    private String supervisorId;
    private String transferType;
    private String currency;
    private String code;
    private String amount;
    private String receiverBank;
    private String receiverAccount;
    private String receiverName;
    private String senderBank;
    private String senderAccount;
    private String senderName;
    private String description;
    private String txnId;
    private String contraAccount;
    private boolean reversal;
    private String ReceiverAddressLine1;
    private String ReceiverAddressLine2;
    private String ReceiverAddressLine3;
    private String ReceiverAddressLine4;
    private String SenderAddressLine1;
    private String SenderAddressLine2;
    private String SenderAddressLine3;
    private String SenderAddressLine4;
}
