package com.service.kyc.ors;

import com.DTO.KYC.ors.AttachmentPayload;
import com.DTO.KYC.ors.PayloadDTO;
import com.DTO.KYC.ors.response.AttachmentAccessData;
import com.DTO.KYC.ors.response.AttachmentData;
import com.dao.kyc.response.ors.AttachmentResponseDTO;
import com.dao.kyc.response.ors.ResponseDTO;
import com.models.kyc.ors.AttachmentEntity;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface AttachmentService {
    AttachmentResponseDTO<AttachmentData> findAttachmentListFromORS(PayloadDTO payload);
    List<AttachmentData> findAttachmentListFromDB(PayloadDTO payload);
    AttachmentAccessData findAttachmentByIdFromBrela(AttachmentPayload payload);
    List<AttachmentEntity> saveAttachment(AttachmentResponseDTO<AttachmentData> attachmentData);
}
