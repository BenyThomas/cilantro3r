package com.DTO.ubx;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
public class ResponseWrapper {
    @JsonProperty("response")
    private String response;
    @JsonProperty("signature")
    private String signature;
    @JsonProperty("MessageResponseCode")
    private String messageResponseCode;
}
