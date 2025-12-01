package com.dao.kyc.response.zcsra;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class DemographicResponse {
    private ResponsePayload payload;
    private String signature;
}
