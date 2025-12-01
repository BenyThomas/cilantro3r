package com.DTO.ubx;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UbxResponse {
    @JsonProperty("responseMessage")
    private String responseMessage;
    @JsonProperty("responseCode")
    private String responseCode;
    @JsonProperty("otac")
    private String otac;
    @JsonProperty("status")
    private String status;

    public UbxResponse(String message,String code){
        this.responseMessage = message;
        this.responseCode = code;
    }
}
