package com.repository;

import com.models.PensionersBatchSummary;
import com.models.Transfers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;
import java.util.Optional;

@Transactional
@Repository
public interface PensionersBatchSummaryRepository extends JpaRepository<PensionersBatchSummary, Long> {
    Optional<PensionersBatchSummary> findByReference(String ref);

}
