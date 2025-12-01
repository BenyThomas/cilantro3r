package com.DTO.KYC.ors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AttachmentPayload {
    @JsonProperty("RegistrationNumber")
    private Long registrationNumber;
    @JsonProperty("EntityType")
    private Long entityType;
    @JsonProperty("attachment_id")
    private String attachmentId;
    @JsonProperty("ApiKey")
   private String apiKey;
}
