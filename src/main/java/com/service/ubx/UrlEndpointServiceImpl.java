package com.service.ubx;

import com.models.ubx.UrlEndpointEntity;
import com.repository.ubx.UrlEndpointRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UrlEndpointServiceImpl implements UrlEndpointService {

    private final UrlEndpointRepository repository;

    public UrlEndpointServiceImpl(UrlEndpointRepository repository) {
        this.repository = repository;
    }

    @Override
    public UrlEndpointEntity createEndpoint(UrlEndpointEntity endpoint) {
        return repository.save(endpoint);
    }

    @Override
    public UrlEndpointEntity getEndpointById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Endpoint not found with id: " + id));
    }

    @Override
    public List<UrlEndpointEntity> getAllEndpoints() {
        return repository.findAll();
    }

    @Override
    public UrlEndpointEntity updateEndpoint(Long id, UrlEndpointEntity endpoint) {
        UrlEndpointEntity existingEndpoint = getEndpointById(id);
        existingEndpoint.setName(endpoint.getName());
        existingEndpoint.setFunction(endpoint.getFunction());
        existingEndpoint.setEndpoint(endpoint.getEndpoint());
        return repository.save(existingEndpoint);
    }

    @Override
    public Optional<UrlEndpointEntity> findEndpointByName(String endpointName) {
        return repository.findByName(endpointName);
    }

    @Override
    public void deleteEndpoint(Long id) {
        repository.deleteById(id);
    }
}

