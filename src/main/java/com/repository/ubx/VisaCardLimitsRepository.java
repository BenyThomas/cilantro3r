package com.repository.ubx;

import com.models.ubx.CardLimitsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisaCardLimitsRepository extends JpaRepository<CardLimitsEntity,Long> {
}
