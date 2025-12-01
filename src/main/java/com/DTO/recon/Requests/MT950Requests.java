package com.DTO.recon.Requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class MT950Requests {
    private Object gls;
    private String accounts;
    private Object transactionRefrences;
    private String reconDate;
    private String amount;
    private String currency;
    private String statementNo;
    private String debitOrCredit;
    private String senderBank;
    private String beneficiaryBank;

}
