package com.dao.kyc.response.ors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class ClassifierResponseDTO<T> {
    @JsonProperty("success")
    private boolean success;
    @JsonProperty("message")
    private String message;
    @JsonProperty("data")
    private List<T> data;
    @JsonProperty("resultcode")
    private int resultCode;
    @Override
    public String toString() {
        return "ResponseDTO{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", data=" + data +
                ", resultCode=" + resultCode +
                '}';
    }
    public static <T> ClassifierResponseDTO<T> fromJson(String json, Class<T> valueType) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructParametricType(ClassifierResponseDTO.class, valueType));
        } catch (Exception e) {
            log.error("Error Occurred when converting AttachmentResponseDTO: {}", e.getMessage()); // Handle the exception as needed
            return null;
        }
    }
}
