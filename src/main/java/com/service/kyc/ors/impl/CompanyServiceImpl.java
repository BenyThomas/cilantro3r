package com.service.kyc.ors.impl;

import com.DTO.KYC.ors.PayloadDTO;
import com.DTO.KYC.ors.response.CompanyData;
import com.DTO.KYC.ors.response.FullResponse;
import com.dao.kyc.response.ors.ResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helper.ORSApiEndpoints;
import com.models.kyc.ors.CompanyEntity;
import com.models.kyc.ors.ORSResponseEntity;
import com.repository.Kyc.ors.CompanyRepository;
import com.repository.Kyc.ors.ResponseRepository;
import com.service.kyc.ors.CompanyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyServiceImpl implements CompanyService {
    private final CompanyRepository companyRepository;
    private final ResponseRepository responseRepository;
    private final ObjectMapper mapper;
    private final ORSApiEndpoints endpoints;
    private final RestTemplate restTemplate;
    @Override
    public CompanyEntity findCompanyEntityInfoById(Long id) {
        return companyRepository.findById(id).orElseThrow(()-> new RuntimeException("Entity not found"));
    }

    @Override
    public ResponseEntity<Object> createCompanyEntity(PayloadDTO payloadDTO) {
        Optional<CompanyEntity> companyEntity = companyRepository
                .findCompanyEntityByCertNumber(
                        String.valueOf(payloadDTO.getRegistrationNumber())
                );
        return companyEntity.<ResponseEntity<Object>>map(entity ->
                ResponseEntity.status(HttpStatus.CONFLICT).body(entity)).orElseGet(() ->
                new ResponseEntity<>(new ResponseDTO<>(true, "Company entity created successfully",
                        null, 0), HttpStatus.CREATED));


    }

    @Override
    public void deleteCompanyEntityById(Long id) {
        companyRepository.deleteById(id);
    }

    @Override
    public Optional<FullResponse<CompanyData>> findCompanyEntityByPayload(PayloadDTO payloadDTO) {
        return Optional.empty();
    }

    @Override
    public ORSResponseEntity findEntityDetailsFromORS(PayloadDTO payloadDTO) {
        HttpEntity<String> entity = endpoints.createEntity(payloadDTO);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(endpoints.getBaseUrl() + endpoints.getEntity(), entity, String.class);
            ORSResponseEntity orsResponseEntity = mapper.readValue(response.getBody(), ORSResponseEntity.class);
            orsResponseEntity.setCertificate(String.valueOf(payloadDTO.getRegistrationNumber()));
            ORSResponseEntity savedOrsEntity = responseRepository.save(orsResponseEntity);
            return savedOrsEntity;
        }catch (Exception e) {
            log.error("Error while processing Request: {}",e.getMessage());
            return null;
        }
    }


    @Override
    public CompanyEntity findCompanyEntityByCertNumber(String certNo) {
        Optional<CompanyEntity> companyEntity = companyRepository.findCompanyEntityByCertNumber(certNo);
        if (companyEntity.isPresent()) {

        }
        return null;
    }

    private ORSResponseEntity saveEntityInLocalDB(FullResponse<CompanyData> fullResponse) {
        Optional<CompanyEntity> companyEntity = companyRepository.findCompanyEntityByCertNumber(fullResponse.getResponseData().getCertNumber());
        if (!companyEntity.isPresent()) {
            ORSResponseEntity entity = mapper.convertValue(fullResponse, ORSResponseEntity.class);
           return responseRepository.save(entity);
        }
        return null;
    }
}
