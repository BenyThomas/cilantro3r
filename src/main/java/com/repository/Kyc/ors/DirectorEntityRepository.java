package com.repository.Kyc.ors;

import org.springframework.data.jpa.repository.JpaRepository;
import com.models.kyc.ors.DirectorEntity;

import java.util.List;

public interface DirectorEntityRepository extends JpaRepository<DirectorEntity, Long> {
    List<DirectorEntity> findByCertificate(String certificate);
}
