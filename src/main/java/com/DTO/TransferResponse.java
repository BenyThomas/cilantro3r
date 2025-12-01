package com.DTO;

import com.helper.TransferItem;
import lombok.Data;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@Data
@XmlRootElement(name = "return")
public class TransferResponse {
    private String result;
    private String reference;
    private String message;
    private String txnId;
    private List<TransferItem> transfers;
}
