package com.DTO.KYC.ors.response;

import com.dao.kyc.response.ors.AttachmentResponseDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
public class AttachmentAccessData {
    @JsonProperty("success")
    private boolean success;
    @JsonProperty("message")
    private String message;
    @JsonProperty("reponseData")
    private String reponseData;
    @JsonProperty("file_content")
    private String fileContent;
    @JsonProperty("Signature")
    private String signature;
    @JsonProperty("resultcode")
    private int resultCode;

    @Override
    public String toString() {
        return "AttachmentAccessData{" +
                "success='" + success + '\'' +
                ", message='" + message + '\'' +
                ", fileContent='" + fileContent + '\'' +
                ", signature='" + signature + '\'' +
                ", resultCode='" + resultCode + '\'' +
                '}';
    }
    public static AttachmentAccessData fromJson(String json) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(json, AttachmentAccessData.class);
        } catch (Exception e) {
            log.error("Error Occurred when converting AttachmentResponseDTO: {}", e.getMessage());
            return null;
        }
    }
}
