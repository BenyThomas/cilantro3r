package com.repository.Kyc.ors;

import org.springframework.data.jpa.repository.JpaRepository;
import com.models.kyc.ors.DirectorEntity;

public interface DirectorRepository extends JpaRepository<DirectorEntity, Long> {
}
