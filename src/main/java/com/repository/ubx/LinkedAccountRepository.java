package com.repository.ubx;

import com.models.ubx.LinkedAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LinkedAccountRepository extends JpaRepository<LinkedAccountEntity, Long> {
}
