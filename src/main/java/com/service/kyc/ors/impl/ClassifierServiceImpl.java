package com.service.kyc.ors.impl;

import com.DTO.KYC.ors.PayloadDTO;
import com.DTO.KYC.ors.response.ClassifierData;
import com.dao.kyc.response.ors.ClassifierResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helper.ORSApiEndpoints;
import com.models.kyc.ors.ClassifierEntity;
import com.repository.Kyc.ors.ClassifierEntityRepository;
import com.service.kyc.ors.ClassifierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClassifierServiceImpl implements ClassifierService {
    private final ClassifierEntityRepository repository;
    private final ObjectMapper objectMapper;
    private final ORSApiEndpoints endpoints;
    private final RestTemplate restTemplate;
    @Override
    public ClassifierEntity createClassifier(ClassifierData data) {
        return repository.save(objectMapper.convertValue(data, ClassifierEntity.class));
    }

    @Override
    public ClassifierEntity updateClassifier(ClassifierData data) {
        return repository.save(objectMapper.convertValue(data, ClassifierEntity.class));
    }

    @Override
    public void deleteClassifier(ClassifierData data) {
         repository.delete(objectMapper.convertValue(data,ClassifierEntity.class));
    }

    @Override
    public ClassifierEntity getClassifier(ClassifierData data) {
        return null;
    }

    @Override
    public List<ClassifierEntity> getClassifiers() {
        return null;
    }

    @Override
    public void createClassifiers(List<ClassifierData> data) {
        List<ClassifierEntity> entities = data.stream()
                .map(dataItem -> objectMapper.convertValue(dataItem, ClassifierEntity.class))
                .collect(Collectors.toList());

        repository.saveAll(entities);
    }

    @Override
    public List<ClassifierData> findClassifiersFromDB(PayloadDTO payloadDTO) {
        HttpEntity<String> entity = endpoints.createEntity(payloadDTO);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(endpoints.getBaseUrl()+endpoints.getAttachmentList(), entity, String.class);
            ClassifierResponseDTO<ClassifierData> classifierData = ClassifierResponseDTO.fromJson(response.getBody(), ClassifierData.class);
            assert classifierData != null;
            createClassifiers(classifierData.getData());
            return classifierData.getData();

        }catch (Exception e) {
            log.error(e.getMessage());
        }


        return Collections.emptyList();
    }

    @Override
    public ClassifierResponseDTO<ClassifierData> findClassifiersFromORS(PayloadDTO payloadDTO) {
        HttpEntity<String> entity = endpoints.createClassifierListEntity(payloadDTO.getApiKey());
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(endpoints.getBaseUrl()+endpoints.getClassifierList(), entity, String.class);
            ClassifierResponseDTO<ClassifierData> classifierData = ClassifierResponseDTO.fromJson(response.getBody(), ClassifierData.class);
            assert classifierData != null;
            createClassifiers(classifierData.getData());
            return classifierData;
        }catch (Exception e) {
            log.error(e.getMessage());
        }
        return new ClassifierResponseDTO<>();
    }
}
