package com.DTO;

import com.helper.TransferItem;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SOAPResponse {
    private String result;
    private String reference;
    private String message;
    private Double availableBalance;
    private Double ledgerBalance;
    private String txnId;

    private List<TransferItem> transfers;
}
