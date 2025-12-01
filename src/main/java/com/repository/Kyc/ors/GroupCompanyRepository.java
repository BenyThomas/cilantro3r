package com.repository.Kyc.ors;

import org.springframework.data.jpa.repository.JpaRepository;
import com.models.kyc.ors.GroupCompanyEntity;

import java.util.Optional;

public interface GroupCompanyRepository extends JpaRepository<GroupCompanyEntity, Long> {
    Optional<GroupCompanyEntity> findByCertificate(String certificate);
}
