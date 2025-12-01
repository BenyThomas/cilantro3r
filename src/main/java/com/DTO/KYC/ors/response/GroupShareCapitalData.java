package com.DTO.KYC.ors.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GroupShareCapitalData {
    @JsonProperty("share_type")
    private String shareType;
    @JsonProperty("total")
    private String total;
    @JsonProperty("currency")
    private String currency;
}
