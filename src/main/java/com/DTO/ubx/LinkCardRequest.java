package com.DTO.ubx;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LinkCardRequest {
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
}
