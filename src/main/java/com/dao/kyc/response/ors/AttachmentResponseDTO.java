package com.dao.kyc.response.ors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentResponseDTO<T> {
    @JsonProperty("success")
    private boolean success;

    @JsonProperty("message")
    private String message;

    @JsonProperty("data")
    private List<T> data;

    @JsonProperty("resultcode")
    private int resultCode;

    // Getters and Setters
    // ... (Include getters and setters for all fields)

    @Override
    public String toString() {
        return "ResponseDTO{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", data=" + data +
                ", resultCode=" + resultCode +
                '}';
    }

    // fromJson method implementation
    public static <T> AttachmentResponseDTO<T> fromJson(String json, Class<T> valueType) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JavaType type = objectMapper.getTypeFactory().constructParametricType(AttachmentResponseDTO.class, valueType);
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.error("Error Occurred when converting AttachmentResponseDTO: {}", e.getMessage()); // Handle the exception as needed
            return null;
        }
    }
}
