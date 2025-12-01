package com.repository.Kyc.ors;

import com.models.kyc.ors.ShareholderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShareholderRepository extends JpaRepository<ShareholderEntity, Long> {
    List<ShareholderEntity> findByCertificate(String certificate);
}
