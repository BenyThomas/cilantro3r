package com.DTO;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
public class CashMovementRequestObj {
    private  String messageType;
    private String txnType;
    private String code;
    private String comments;
    private String reference;
    private BigDecimal amount;
    private String sourceAcc;
    private String sourceAccName;
    private String destinationAcc;
    private String destinationAccName;
    private String iniatedBy;
    private String iniatedDate;
    private String approvedBy;
    private String approvedDate;

}
