package com.DTO.Teller;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
@Getter
@Setter
@ToString
public class VikobaUpdateRequest {
        private String transactionID;
        private String mno;
        private String mnoReceipt;
        private String amount;
        private String messageType;
}
