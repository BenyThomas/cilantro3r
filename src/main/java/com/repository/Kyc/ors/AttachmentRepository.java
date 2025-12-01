package com.repository.Kyc.ors;

import com.models.kyc.ors.AttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<AttachmentEntity, Long> {
    List<AttachmentEntity> findAttachmentEntitiesByRegistrationNumber(String registrationNumber);

}
