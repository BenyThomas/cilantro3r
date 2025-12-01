package com.DTO.ubx;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UnblockCardRequest {
    @JsonProperty("institutionId")
    private String institutionId;
    @JsonProperty("userIdentification")
    private String userIdentification;
    @JsonProperty("channelId")
    private String channelId;
    @JsonProperty("customerId")
    private String customerId;
    @JsonProperty("cardNumber")
    private String cardNumber;
    @JsonProperty("userAction")
    private String userAction;
    @JsonProperty("responseCode")
    private String responseCode;
}
