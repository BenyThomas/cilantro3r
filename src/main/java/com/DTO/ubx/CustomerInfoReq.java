package com.DTO.ubx;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CustomerInfoReq {
    @JsonProperty("lastName")
    private String lastName;
    @JsonProperty("cif")
    private String cif;
    @JsonProperty("firstName")
    private String firstName;
    @JsonProperty("nationalId")
    private String nationalId;
    @JsonProperty("accountType")
    private String accountType;
    @JsonProperty("msisdn")
    private String msisdn;
    @JsonProperty("accountNumber")
    private String accountNumber;
    @JsonProperty("cardNumber")
    private String cardNumber;
    @JsonProperty("channelId")
    private String channelId;
    @JsonProperty("customerName")
    private String customerName;
    @JsonProperty("username")
    private String currentUserName;
    @JsonProperty("branchCode")
    private String branchCode;
    @JsonProperty("newPin")
    private String newPin;
    @JsonProperty("panExpireDate")
    private String panExpireDate;
    @JsonProperty("email")
    private String email;
    @JsonIgnore
    private String charge;
    @JsonProperty("custId")
    private String custId;
    @JsonProperty("customerRim")
    private String customerRim;
    @JsonProperty("category")
    private String category;
    @JsonIgnore
    private int retryCount = 0;

}

