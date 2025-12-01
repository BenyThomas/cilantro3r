package com.DTO.ubx;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RequestWrapper {
    @JsonProperty("request")
    private String request;
    @JsonProperty("signature")
    private String signature;
}
