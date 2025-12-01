package com.helper;

import com.DTO.KYC.ors.response.AttachmentData;
import com.DTO.ubx.EventTrail;
import com.DTO.ubx.InstantCardSummary;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.models.kyc.ors.AttachmentEntity;
import com.models.ubx.CardActionStatus;
import com.models.ubx.CardDetailsEntity;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

@Slf4j
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Mapper {

    private static final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public static String toJson(Object obj) {
        JSONObject jsonObject = new JSONObject(obj);
        return jsonObject.toString();
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Error while converting {} Json String to {} : {}", json, clazz, e.getMessage());
            throw new MapperException("Failed to convert JSON string to object", e);
        }
    }

    public static String toJsonJackson(Object obj) {
        try {
            log.debug("Object to be converted to JSON using Jackson: {}", obj);
            String json = objectMapper.writeValueAsString(obj);
            return json;
        } catch (JsonProcessingException e) {
            log.error("Error while converting object to JSON using Jackson: {}", obj, e);
            throw new MapperException("Failed to convert object to JSON string", e);
        } catch (Exception e) {
            log.error("Unexpected error while converting object to JSON: {}", obj, e);
            throw new RuntimeException("Unexpected error during JSON conversion", e);
        }
    }

    public static <T> T fromJsonJackson(String json, Class<T> clazz) {
        try {
            log.debug("JSON String to be converted to object using Jackson: {}", json);
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Error while converting JSON String to object using Jackson: {}", json, e);
            throw new MapperException("Failed to convert JSON string to object", e);
        }
    }
    public static AttachmentData toAttachmentData(AttachmentEntity attachmentEntity) {
        return objectMapper.convertValue(attachmentEntity, AttachmentData.class);
    }
    public static InstantCardSummary toSummary(CardDetailsEntity c) {
        InstantCardSummary s = new InstantCardSummary();
        s.setId(c.getId());
        s.setReference(c.getReference());
        s.setPan(c.getPan());
        s.setAccountNo(c.getAccountNo());
        s.setCustomerName(c.getCustomerName());
        s.setPhone(c.getPhone());
        s.setEmail(c.getEmail());
        s.setCustId(c.getCustid());
        s.setCustomerRimNo(c.getCustomerRirmNo());
        s.setShortName(c.getCustomerShortName());
        s.setCategory(c.getCustomerCategory());
        s.setCreatedAt(c.getCreatedDt());
        s.setStatus(c.getStatus());
        s.setStage(c.getStage());
        s.setOriginatingBranch(c.getOriginatingBranch());
        s.setCollectingBranch(c.getCollectingBranch());
        return s;
    }
    public static EventTrail toEventDTO(CardActionStatus a) {
        EventTrail d = new EventTrail();
        d.setId(a.getId());
        d.setStep(a.getStep() != null ? a.getStep().name() : null);
        d.setStatus(a.getStatus());
        d.setMessage(a.getMessage());
        d.setUpdatedAt(a.getUpdatedAt());
        d.setRequestId(a.getRequestId());
        return d;
    }
}

// Custom Exception
