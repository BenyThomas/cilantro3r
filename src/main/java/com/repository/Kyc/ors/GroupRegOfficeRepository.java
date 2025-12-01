package com.repository.Kyc.ors;

import com.DTO.KYC.ors.response.GroupRegOffice;
import com.models.kyc.ors.GroupRegOfficeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupRegOfficeRepository extends JpaRepository<GroupRegOfficeEntity, Long> {
    Optional<GroupRegOfficeEntity> findByCertificate(String certificate);
}
