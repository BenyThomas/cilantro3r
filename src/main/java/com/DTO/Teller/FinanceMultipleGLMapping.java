package com.DTO.Teller;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class FinanceMultipleGLMapping {
    private String glAcctNo;
    private String amount;
    private String vat;
}
