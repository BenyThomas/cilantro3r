package com.helper;

import com.DTO.KYC.ors.AttachmentAccessDTO;
import com.DTO.KYC.ors.AttachmentPayload;
import com.DTO.KYC.ors.PayloadDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Data
@Component
@Slf4j
@RequiredArgsConstructor
public class ORSApiEndpoints {
    @Value("${brela.ors.api.key:-1}") private String apiKye;
    @Value("${brela.ors.api.base.url:http://172.21.2.12:8082/api}") private String baseUrl;
    private String entity = "/entityInfoByType";
    private String classifierList = "/classifier_list";
    private String classifier = "/classfiers";
    private String attachmentList = "/attachments_list";
    private String attachment = "/access_attachment";
    private final ObjectMapper objectMapper;
    @Value("${attachment.server.url:-1}")
    private String attachmentServerUrl;
    @Value("${attachment.download.endpoint:-1}")
    private String attachmentDownloadEndpoint;
    @Value("${attachment.save.endpoint:-1}")
    private String attachmentSaveEndpoint;

    public String toJson(Object obj) {
        try {
            log.info("In toJson:{}", objectMapper.writeValueAsString(obj));
            return objectMapper.writeValueAsString(obj);

        } catch (JsonProcessingException e) {
            log.error("Error while converting object to json: {}", e.getMessage());
            return null;
        }
    }
    public Object fromJson(String json) {
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            log.error("Error while converting json to object: {}", e.getMessage());
            return null;
        }
    }
    public  HttpEntity<String> createEntity(PayloadDTO payloadDTO) {
        payloadDTO.setApiKey(apiKye);
        String payload = Mapper.toJsonJackson(payloadDTO);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(payload, headers);
    }
    public  HttpEntity<String> createAttachmentAccessEntity(AttachmentPayload payloadDTO) {
        payloadDTO.setApiKey(apiKye);
        String payload = Mapper.toJsonJackson(payloadDTO);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(payload, headers);
    }
    public  HttpEntity<String> createAttachmentEntity(AttachmentPayload payloadDTO) {
        payloadDTO.setApiKey(apiKye);
        String payload = Mapper.toJsonJackson(payloadDTO);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(payload, headers);
    }
    public  HttpEntity<String> attachmentEntity(AttachmentAccessDTO payloadDTO) {
        String payload = Mapper.toJsonJackson(payloadDTO);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(payload, headers);
    }
    public HttpEntity<String> createClassifierListEntity(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String payload = String.format("{\"ApiKey\":\"%s\"}", apiKey);
        return new HttpEntity<>(payload, headers);
    }

    public ResponseEntity<String> sendRequest(HttpEntity<String> httpEntity, RestTemplate restTemplate, String baseUrl, String endpoint) {
        log.info("Request with {} Send to: {}", httpEntity, baseUrl+endpoint);
        ResponseEntity<String> response = restTemplate.postForEntity(baseUrl+endpoint, httpEntity, String.class);
        log.info("Response with {} returned from: {}", response, baseUrl+endpoint);
        return response;
    }

    public String extractDataFromString(String json, String path){
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            JsonNode jsonNode = objectMapper.readTree(json);
            String[] keys = path.split("\\.");

            JsonNode currentNode = jsonNode;
            for (String key : keys) {
                if (currentNode == null || !currentNode.has(key)) {
                    return null;
                }
                currentNode = currentNode.get(key);
            }

            // Return a pretty-printed string for arrays or objects
            if (currentNode.isArray() || currentNode.isObject()) {
                return currentNode.toPrettyString();
            }

            // Return a string for scalar values
            return currentNode.asText();
        }catch (IOException e) {
            log.error("Error while extracting data from json: {}", e.getMessage());
            return null;
        }
    }

}
