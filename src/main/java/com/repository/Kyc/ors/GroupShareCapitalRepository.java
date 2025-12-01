package com.repository.Kyc.ors;

import com.models.kyc.ors.GroupShareCapitalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupShareCapitalRepository extends JpaRepository<GroupShareCapitalEntity, Long> {
    Optional<GroupShareCapitalEntity> findByCertificate(String certificate);
}
