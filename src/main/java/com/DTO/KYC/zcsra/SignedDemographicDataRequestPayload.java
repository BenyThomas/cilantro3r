package com.DTO.KYC.zcsra;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignedDemographicDataRequestPayload {
    private DemographicDataRequestPayload payload;
    private String signature;
}
