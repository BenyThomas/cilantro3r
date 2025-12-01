package com.repository.Kyc.ors;

import org.springframework.data.jpa.repository.JpaRepository;
import com.models.kyc.ors.CMDetailedInfoEntity;

import java.util.Optional;

public interface CMDetailedInfoRepository extends JpaRepository<CMDetailedInfoEntity, Long> {
    Optional<CMDetailedInfoEntity> findByCertificate(String certificate);
}
