package com.DTO.mx;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class Txn {
    private String reference;
    private String priority;
    private String currency;
    private String amount;
    private String chargeBearer;
    private String beneficiaryName;
    private String beneficiaryAddress;
    private String beneficiaryAccount;
    private String ordererName;
    private String ordererAddress;
    private String ordererAccount;
    private String purposeCode;
    private String remittanceInformation;
}
