package com.repository.Kyc.ors;

import com.models.kyc.ors.BusinessActivityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BusinessActivityRepository extends JpaRepository<BusinessActivityEntity, Long> {
    List<BusinessActivityEntity> findByCertificate(String certificate);
}
