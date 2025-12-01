package com.helper;

import com.DTO.KYC.ors.response.BusinessActivityData;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BusinessActivityDeserializer extends JsonDeserializer<List<BusinessActivityData>> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<BusinessActivityData> deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        List<BusinessActivityData> businessActivities = new ArrayList<>();

        if (node.isArray()) {
            // If it's already an array, deserialize normally
            for (JsonNode element : node) {
                businessActivities.add(objectMapper.treeToValue(element, BusinessActivityData.class));
            }
        } else if (node.isObject()) {
            // If it's a single object, wrap it in a list
            businessActivities.add(objectMapper.treeToValue(node, BusinessActivityData.class));
        }

        return businessActivities;
    }
}
