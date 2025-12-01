package com.repository.Kyc.ors;

import org.springframework.data.jpa.repository.JpaRepository;
import com.models.kyc.ors.GroupCompanySecretaryEntity;

import java.util.Optional;

public interface GroupCompanySecretaryRepository extends JpaRepository<GroupCompanySecretaryEntity,Long> {
    Optional<GroupCompanySecretaryEntity> findByCertificate(String certificate);
}
