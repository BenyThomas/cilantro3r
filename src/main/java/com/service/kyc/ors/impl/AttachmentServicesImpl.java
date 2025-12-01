package com.service.kyc.ors.impl;

import com.DTO.KYC.ors.AttachmentAccessDTO;
import com.DTO.KYC.ors.AttachmentPayload;
import com.DTO.KYC.ors.PayloadDTO;
import com.DTO.KYC.ors.response.AttachmentAccessData;
import com.DTO.KYC.ors.response.AttachmentData;
import com.config.EndPoints;
import com.dao.kyc.response.ors.AttachmentResponseDTO;
import com.dao.kyc.response.ors.ResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helper.Mapper;
import com.helper.ORSApiEndpoints;
import com.models.kyc.ors.AttachmentEntity;
import com.repository.Kyc.ors.AttachmentRepository;
import com.service.kyc.ors.AttachmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentServicesImpl implements AttachmentService {
    private final AttachmentRepository attachmentRepository;
    private final RestTemplate restTemplate;
    private final ORSApiEndpoints endPoints;
    private final ObjectMapper mapper;
    @Override
    public AttachmentResponseDTO<AttachmentData> findAttachmentListFromORS(PayloadDTO payload) {
        String message = "1";
        HttpEntity<String> entity = endPoints.createEntity(payload);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(endPoints.getBaseUrl()+endPoints.getAttachmentList(),entity, String.class);
            AttachmentResponseDTO<AttachmentData> attachmentList= AttachmentResponseDTO.fromJson(response.getBody(), AttachmentData.class);
            AttachmentPayload payload1 = new AttachmentPayload();
            payload1.setApiKey(payload.getApiKey());
            payload1.setRegistrationNumber(payload.getRegistrationNumber());
            payload1.setEntityType(payload.getEntityType());
            if (attachmentList != null && attachmentList.getData() != null) {
                if (attachmentList.isSuccess()){
                    saveAttachment(attachmentList);
                }
                for (AttachmentData attachmentData : attachmentList.getData()) {
                    payload1.setAttachmentId(attachmentData.getAttachmentId());
                    HttpEntity<String> accessEntity = endPoints.createAttachmentAccessEntity(payload1);
                    ResponseEntity<String> response1 = restTemplate.postForEntity(endPoints.getBaseUrl()+endPoints.getAttachment(), accessEntity, String.class);
                    AttachmentAccessData accessData = AttachmentAccessData.fromJson(response1.getBody());
                    //Save the Attachment content to Attachment Server
                    AttachmentAccessDTO accessDTO = new AttachmentAccessDTO();
                    if (accessData!=null && accessData.getFileContent() != null){
                        accessDTO.setContent(accessData.getFileContent());
                        accessDTO.setContentType(attachmentData.getFileType());
                        accessDTO.setSize((attachmentData.getFileSize() != null && !attachmentData.getFileSize().trim().isEmpty())
                                ? Long.parseLong(attachmentData.getFileSize().trim())
                                : 0L);
                        accessDTO.setFilename(attachmentData.getFileName());
                        accessDTO.setThirdPartId(attachmentData.getAttachmentId());
                        accessDTO.setProjectName("CILANTRO");
                        accessDTO.setProjectCode("BRELA-TCB");
                        accessDTO.setEntityRegNo(String.valueOf(payload1.getRegistrationNumber()));
                        HttpEntity<String> attachmentEntity = endPoints.attachmentEntity(accessDTO);
                        ResponseEntity<String> response2 = restTemplate.postForEntity(endPoints.getAttachmentServerUrl()+"/api/v1/attachments/save/brela", attachmentEntity, String.class);
                        attachmentData.setLocalAttachmentId(response2.getBody());
                    }
                }
            }
            return attachmentList;
        }catch (Exception e){
            message  = e.getMessage();
            log.error("Error: {}",e.getMessage());
        }
        return new AttachmentResponseDTO<>(false,message,null,900);
    }

    @Override
    public List<AttachmentData> findAttachmentListFromDB(PayloadDTO payload) {
        return attachmentRepository.findAttachmentEntitiesByRegistrationNumber(String.valueOf(payload.getRegistrationNumber()))
                .stream().map(Mapper::toAttachmentData).collect(Collectors.toList());
    }

    @Override
    public AttachmentAccessData findAttachmentByIdFromBrela(AttachmentPayload payload) {
        HttpEntity<String> entity = endPoints.createAttachmentEntity(payload);
        try {
            ResponseEntity<AttachmentAccessData> response = restTemplate.postForEntity(endPoints.getBaseUrl()+endPoints.getAttachment(),entity, AttachmentAccessData.class);
            return response.getBody();
        }catch (Exception e){
            log.error("Error: {}",e.getMessage());
        }
        return null;
    }

    @Override
    public List<AttachmentEntity> saveAttachment(AttachmentResponseDTO<AttachmentData> attachmentData) {
        List<AttachmentEntity> attachmentEntities = new ArrayList<>();
        for (AttachmentData data : attachmentData.getData()) {
            AttachmentEntity attachmentEntity = mapper.convertValue(data, AttachmentEntity.class);
            attachmentEntities.add(attachmentEntity);
        }
        return attachmentRepository.saveAll(attachmentEntities);
    }
}
