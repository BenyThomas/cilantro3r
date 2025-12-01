package com.controller.ubx;

import com.models.ubx.UrlEndpointEntity;
import com.service.ubx.UrlEndpointService;
import io.swagger.annotations.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/url-endpoints")
public class UrlEndpointController {

    private final UrlEndpointService service;

    public UrlEndpointController(UrlEndpointService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<UrlEndpointEntity> createEndpoint(@RequestBody UrlEndpointEntity endpoint) {
        return ResponseEntity.ok(service.createEndpoint(endpoint));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UrlEndpointEntity> getEndpointById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getEndpointById(id));
    }
    @GetMapping("/endpoint/{name}")
    public ResponseEntity<UrlEndpointEntity> getEndpointByName(@PathVariable String name) {
        Optional<UrlEndpointEntity> endpoint = service.findEndpointByName(name);
        return endpoint.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
    @GetMapping
    public ResponseEntity<List<UrlEndpointEntity>> getAllEndpoints() {
        return ResponseEntity.ok(service.getAllEndpoints());
    }

    @PutMapping("/{id}")
    public ResponseEntity<UrlEndpointEntity> updateEndpoint(@PathVariable Long id, @RequestBody UrlEndpointEntity endpoint) {
        return ResponseEntity.ok(service.updateEndpoint(id, endpoint));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEndpoint(@PathVariable Long id) {
        service.deleteEndpoint(id);
        return ResponseEntity.noContent().build();
    }
}
