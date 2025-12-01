package com.service.ubx;

import com.models.ubx.UrlEndpointEntity;

import java.util.List;
import java.util.Optional;

public interface UrlEndpointService {
    UrlEndpointEntity createEndpoint(UrlEndpointEntity endpoint);
    UrlEndpointEntity getEndpointById(Long id);
    List<UrlEndpointEntity> getAllEndpoints();
    UrlEndpointEntity updateEndpoint(Long id, UrlEndpointEntity endpoint);
    Optional<UrlEndpointEntity> findEndpointByName(String endpointName);
    void deleteEndpoint(Long id);
}
