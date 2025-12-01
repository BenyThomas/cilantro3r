package com.DTO.KYC.zcsra;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BiometricRequestPayload {
    @JsonIgnore
    @Value("${api.key}")
    private String apiKey;
    private String zanID;
    private String cardholderPhoto;  // Base64-encoded photo
    private String cardholderFinger;  // Base64-encoded fingerprint
    private int cardholderFingerPosition;
}
