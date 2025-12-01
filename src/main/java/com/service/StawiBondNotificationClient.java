package com.service;

import com.DTO.stawi.StawiBondLookupRequest;
import com.DTO.stawi.StawiBondLookupResponse;
import com.DTO.stawi.StawiBondNotificationRequest;
import com.config.SYSENV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StawiBondNotificationClient {

    private static final Logger log = LoggerFactory.getLogger(StawiBondNotificationClient.class);

    private final RestTemplate restTemplate;
    private final SYSENV sysEnv;

    public StawiBondNotificationClient(RestTemplate restTemplate, SYSENV sysEnv) {
        this.restTemplate = restTemplate;
        this.sysEnv = sysEnv;
    }

    public ResponseEntity<StawiBondLookupResponse> send(StawiBondNotificationRequest body) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        List<MediaType> accepts = new ArrayList<>();
        accepts.add(MediaType.APPLICATION_JSON);
        headers.setAccept(accepts);
        HttpEntity<StawiBondNotificationRequest> entity = new HttpEntity<>(body, headers);

        try {
            return restTemplate.postForEntity(sysEnv.TCB_STAWI_BOND_NOTIFICATION_URL, entity, StawiBondLookupResponse.class);
        } catch (RestClientException ex) {
            log.error("Failed to send Stawi bond notification to {}: {}", sysEnv.TCB_STAWI_BOND_NOTIFICATION_URL, ex.getMessage(), ex);
            throw ex;
        }
    }
    public StawiBondLookupResponse lookup(String dseNumber) {
        Map<String, String> params = new HashMap<>();
        params.put("dseAccount", dseNumber);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        List<MediaType> accepts = new ArrayList<>();
        accepts.add(MediaType.APPLICATION_JSON);
        headers.setAccept(accepts);
        HttpEntity<Map<String,String>> entity = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<StawiBondLookupResponse> resp =
                    restTemplate.postForEntity(sysEnv.TCB_STAWI_BOND_LOOKUP_URL, entity, StawiBondLookupResponse.class);

            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                return resp.getBody();
            }
            log.warn("Lookup call returned non-OK status: {}", resp.getStatusCode());
            return new StawiBondLookupResponse("Error","Lookup call returned non-OK status: " +resp.getStatusCode(),"96",null);
        } catch (HttpStatusCodeException e) {
            log.error("Lookup failed: HTTP {} - body: {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            return new StawiBondLookupResponse("ERROR",e.getMessage(),e.getStatusCode().name(), null);
        } catch (Exception e) {
            log.error("Lookup failed: {}", e.getMessage(), e);
            return new StawiBondLookupResponse("ERROR","Unknown error","96",null);
        }
    }
}
