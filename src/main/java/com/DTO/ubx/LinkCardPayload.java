package com.DTO.ubx;

import lombok.Data;

@Data
public class LinkCardPayload {
    private String cardNumber;
    private String accountNumber;
    private String userName;
    private String nationalID;
    private String c1_FirstName;
    private String c1_SecondName;
    private String c1_LastName;
    private String channelId;
    private String customerId;
    private String mobileTelephoneNumber;
    private String emailAddress;
    private String postalCity;
    private String postalRegion;
}
