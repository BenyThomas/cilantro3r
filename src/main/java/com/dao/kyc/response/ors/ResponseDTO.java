package com.dao.kyc.response.ors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResponseDTO<T> {
    @JsonProperty("success")
    private boolean success;
    @JsonProperty("message")
    private String message;
    @JsonProperty("reponseData")
    private T data;
    @JsonProperty("resultcode")
    private int resultCode;


    public static <T> ResponseDTO<T> fromJson(String json, Class<T> valueType) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructParametricType(ResponseDTO.class, valueType));
        } catch (Exception e) {
            log.error("Error while converting ResponseDTO: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Converts JSON string into ResponseDTO with List<T>, ensuring correct deserialization.
     */
    public static <T> ResponseDTO<List<T>> fromJsonAsList(String json, Class<T> valueType) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode rootNode = objectMapper.readTree(json);
            JsonNode dataNode = rootNode.get("data");

            if (dataNode == null) {
                log.error("Error: data field is null in JSON.");
                return null;
            }

            ResponseDTO<List<T>> responseDTO = new ResponseDTO<>();
            responseDTO.setSuccess(rootNode.get("success").asBoolean());
            responseDTO.setMessage(rootNode.get("message").asText());
            responseDTO.setResultCode(rootNode.get("resultcode").asInt());

            // Create correct type reference for List<T>
            JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, valueType);

            // If data is already a list, deserialize normally
            if (dataNode.isArray()) {
                List<T> dataList = objectMapper.readValue(dataNode.traverse(), listType);
                responseDTO.setData(dataList);
            } else {
                // If data is a single object, wrap it in a list
                List<T> singleItemList = new ArrayList<>();
                singleItemList.add(objectMapper.treeToValue(dataNode, valueType));
                responseDTO.setData(singleItemList);
            }

            return responseDTO;
        } catch (Exception e) {
            log.error("Error while converting ResponseDTO<List<T>>: {}", e.getMessage());
            return null;
        }
    }
}
