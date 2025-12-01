package com.repository.ubx;

import com.models.ubx.UrlEndpointEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UrlEndpointRepository extends JpaRepository<UrlEndpointEntity, Long> {
    Optional<UrlEndpointEntity> findByName(String name);
}
