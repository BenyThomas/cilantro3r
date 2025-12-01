package com.repository.ubx;

import com.models.ubx.CardDetailsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisaCardDetailsRepository extends JpaRepository<CardDetailsEntity, Long> {
}
