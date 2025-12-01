package com.DTO.KYC.ors.response;

import com.dao.kyc.response.ors.ResponseDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
public class FullResponse<T> {
    @JsonProperty("success")
    private boolean success;
    @JsonProperty("message")
    private String message;
    @JsonProperty("reponseData")
    private T responseData;
    @JsonProperty("resultcode")
    private int resultCode;

    public static <T> FullResponse<T> fromJson(String json, Class<T> valueType) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructParametricType(FullResponse.class, valueType));
        } catch (Exception e) {
            log.error("Error while converting FullResponse: {}", e.getMessage()); // Handle the exception as needed
            return null;
        }
    }
}
