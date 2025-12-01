package com.repository.Kyc.ors;

import com.DTO.KYC.ors.CompanyDTO;
import com.DTO.KYC.ors.ORSResponseProjection;
import com.dao.kyc.response.ors.ResponseDTO;
import com.models.kyc.ors.ORSResponseEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ResponseRepository extends JpaRepository<ORSResponseEntity, Long> {
  Optional<ORSResponseEntity> findByCertificate(String certificate);
}
