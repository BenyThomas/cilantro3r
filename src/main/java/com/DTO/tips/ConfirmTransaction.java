package com.DTO.tips;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfirmTransaction {
    private String payerRef;
    private String payerIdentifier;
    private String payerFullName;
    private String payeeIdentifier;
    private String payeeFullName;
    private String amount;
    private String currency;
    private String transferState;
    private String transferStateReason;
    private String payeeRef;
    private String transferDate;
    private String completedDate;
}
