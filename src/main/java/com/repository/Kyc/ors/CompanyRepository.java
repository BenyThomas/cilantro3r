package com.repository.Kyc.ors;

import com.DTO.KYC.ors.ORSResponseProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.models.kyc.ors.CompanyEntity;

import java.util.Optional;

@Repository
@EnableJpaRepositories
public interface CompanyRepository extends JpaRepository<CompanyEntity, Long> {
    Optional<CompanyEntity> findCompanyEntityByCertNumber(String cerNumber);
}
