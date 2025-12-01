package com.repository;

import com.models.VisaCardBINConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Transactional
@Repository
public interface VisaCardBINConfigurationRepository extends JpaRepository<VisaCardBINConfiguration, Long> {
    Optional<VisaCardBINConfiguration> findByRefKey(String ref);
    List<VisaCardBINConfiguration> findByStatus(String status);

}
