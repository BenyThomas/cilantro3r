package com.DTO.psssf;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class LoanVerificationReq {
    private String reference;
    private String loanAmount;
    private String pensionerId;
    private String pensionerName;
    private String pensionerRubikonName;
    private String monthlyInst;
    private String period;
    private String narration;
    private String bankType;
    private String initiatedBy;
    private String submittedBy;
    private String accNumber;
    private String branchCode;
    private String branchName;
    private String create_dt;
    private byte[] clearanceDoc;
    private byte[] changeBankAccDoc;
}
