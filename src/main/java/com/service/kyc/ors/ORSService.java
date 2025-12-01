package com.service.kyc.ors;



import com.DTO.KYC.ors.AttachmentPayload;
import com.DTO.KYC.ors.ClassifierPayload;
import com.DTO.KYC.ors.PayloadDTO;
import com.DTO.KYC.ors.response.*;
import com.dao.kyc.response.ors.AllResponseDTO;
import com.dao.kyc.response.ors.AttachmentResponseDTO;
import com.dao.kyc.response.ors.ClassifierResponseDTO;
import com.dao.kyc.response.ors.ResponseDTO;

import java.util.List;
import java.util.Map;

public interface ORSService {
    FullResponse getEntityInfoByType(PayloadDTO payloadDTO);
    List<ClassifierData> getClassifiedList(String apiKey);
    ClassifierData getClassifier(ClassifierPayload payload);
    List<AttachmentData> getAttachmentList(PayloadDTO payloadDTO);
    AttachmentAccessData getAttachmentAccess(AttachmentPayload payload);
    AllResponseDTO<AttachmentResponseDTO<AttachmentData>, ClassifierResponseDTO<ClassifierData>, ResponseDTO<CompanyData>> findCompiledResponse(PayloadDTO payloadDTO);
    byte[] getEntityDetailsInPdf(CompanyData companyData);
    String convertToPdfAndSave(CompanyData companyData);
    Map<String, Object> findEntityDetails(PayloadDTO payloadDTO);


    CompanyData getEntityInfo(String certNo);
}
