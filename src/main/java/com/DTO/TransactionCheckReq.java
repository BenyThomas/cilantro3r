package com.DTO;

import lombok.Data;

@Data
public class TransactionCheckReq {
    private String reference;
    private String amount;
}
