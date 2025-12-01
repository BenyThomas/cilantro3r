package com.DTO.KYC.ors;



import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;


@Data
public class PayloadDTO {

    @JsonProperty("RegistrationNumber")
    private Long registrationNumber;
    @JsonProperty("EntityType")
    private Long entityType;
    @JsonProperty("ApiKey")
    private String apiKey;
}
