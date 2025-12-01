package com.DTO.ubx;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReissueCardPinRequest {
    @JsonProperty("institutionId")
    private String institutionId;
    @JsonProperty("userIdentification")
    private String userIdentification;
    @JsonProperty("channelId")
    private String channelId;
    @JsonProperty("cardNumber")
    private String cardNumber;
}
