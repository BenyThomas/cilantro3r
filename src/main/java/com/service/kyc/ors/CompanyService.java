package com.service.kyc.ors;

import com.DTO.KYC.ors.EntityInfoDTO;
import com.DTO.KYC.ors.PayloadDTO;
import com.DTO.KYC.ors.response.CompanyData;
import com.DTO.KYC.ors.response.FullResponse;
import com.models.kyc.ors.CompanyEntity;
import com.models.kyc.ors.ORSResponseEntity;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

public interface CompanyService {
    CompanyEntity findCompanyEntityInfoById(Long id);
    ResponseEntity<Object> createCompanyEntity(PayloadDTO payloadDTO);
    void deleteCompanyEntityById(Long id);
    Optional<FullResponse<CompanyData>> findCompanyEntityByPayload(PayloadDTO payloadDTO);
    ORSResponseEntity findEntityDetailsFromORS(PayloadDTO payloadDTO);
    CompanyEntity findCompanyEntityByCertNumber(String certNo);
}
