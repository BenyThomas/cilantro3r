package com.DTO;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
public class LoanRepayment {
    private String loanType;
    private String debitAccount;
    private BigDecimal amount;
    private String  cbsReference;
    private String  repaymentDate;
    private String  loanId;
    private String msisdn;
    private String narration;
}
