package com.DTO.ubx;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PinChangeRequest {
    private String institutionId;
    private String userIdentification;
    private String channelId;
    private String cardNumber;
    @JsonProperty("currentPin")
    private String currentPIN;
    @JsonProperty("newPin")
    private String newPIN;
    private String track2Data;
}
