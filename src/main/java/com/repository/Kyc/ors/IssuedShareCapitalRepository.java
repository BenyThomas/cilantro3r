package com.repository.Kyc.ors;

import com.models.kyc.ors.IssuedShareCapitalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IssuedShareCapitalRepository extends JpaRepository<IssuedShareCapitalEntity, Long> {
    Optional<IssuedShareCapitalEntity> findByCertificate(String certificate);
}
