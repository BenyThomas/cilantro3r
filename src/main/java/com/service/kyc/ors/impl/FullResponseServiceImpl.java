package com.service.kyc.ors.impl;

import com.DTO.KYC.ors.PayloadDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.service.kyc.ors.CompanyService;
import com.service.kyc.ors.FullResponseService;
import com.service.kyc.ors.ORSService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FullResponseServiceImpl implements FullResponseService {
    private final CompanyService companyService;
    private final ORSService orsService;
    private final ObjectMapper mapper;
    @Override
    public void processFullResponse(PayloadDTO payload) {
        companyService.createCompanyEntity(payload);

    }
}
