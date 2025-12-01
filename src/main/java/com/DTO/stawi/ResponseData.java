package com.DTO.stawi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponseData {
    private String investorName;
    private String investorPhoneNumber;
    private String csdAccount;
    private String brokerCode;
    private String messageId;
    private String gender;
    private String dateOfBirth;
    private String email;
    private String address1;
    private String address2;
    private String address3;
    private String address4;
    private String registrationNumber;
    private String clientType;
    private String postCode;
    private String contactPerson;
    private String telNumber;
    private String accountNumber;
    private String bankCode;
    private String incomeBankName;
    private String incomeAccountNumber;
    private String hasAccount;
    private String brokerId;

}