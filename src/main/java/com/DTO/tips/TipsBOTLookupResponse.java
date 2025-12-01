package com.DTO.tips;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TipsBOTLookupResponse {
    private String payerRef;
    private Object payer;
    private Object payee;
    private Object amount;
    private Object endUserFee;
    private Object transactionType;
    private String lookupId;
    private String transferState;
    private String payeeRef;
    private String switchRef;
    private String transferDate;
    private String completedDate;
    private String description;
    private Object transactionCategory;
    private Object settlementWindow;

    @Override
    public String toString() {
        return "TipsBOTLookupResponse{" +
                "payerRef='" + payerRef + '\'' +
                ", payer=" + payer +
                ", payee=" + payee +
                ", amount=" + amount +
                ", endUserFee=" + endUserFee +
                ", transactionType=" + transactionType +
                ", lookupId='" + lookupId + '\'' +
                ", transferState='" + transferState + '\'' +
                ", payeeRef='" + payeeRef + '\'' +
                ", switchRef='" + switchRef + '\'' +
                ", transferDate='" + transferDate + '\'' +
                ", completedDate='" + completedDate + '\'' +
                ", description='" + description + '\'' +
                ", transactionCategory=" + transactionCategory +
                ", settlementWindow=" + settlementWindow +
                '}';
    }
}
