package com.DTO.KYC.zcsra;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignedBiometricRequestPayload {
    private BiometricRequestPayload payload;
    private String signature;
}
