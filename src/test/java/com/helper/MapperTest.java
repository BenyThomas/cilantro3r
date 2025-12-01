package com.helper;

import com.DTO.KYC.ors.PayloadDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MapperTest {

    @Test
    void toJson() {
    }

    @Test
    void fromJson() {
    }

    @Test
    void toJsonJackson() {
        PayloadDTO dto = new PayloadDTO();
        dto.setRegistrationNumber(12345L);
        dto.setEntityType(1L);
        dto.setApiKey("poiueworiwruywe87qwa");

        try {
            String expectedJson = "{\n" +
                    "  \"RegistrationNumber\" : 12345,\n" +
                    "  \"EntityType\" : 1,\n" +
                    "  \"ApiKey\" : \"poiueworiwruywe87qwa\"\n" +
                    "}";

            String jsonString = Mapper.toJsonJackson(dto);

            assertEquals(expectedJson, jsonString.replace("\r\n", "\n"));
        } catch (MapperException e) {
            fail("Serialization failed: " + e.getMessage());
        }
    }

    @Test
    void fromJsonJackson() {
            String json = "{\n  \"RegistrationNumber\" : \"12345\",\n  \"EntityType\" : \"1\"\n}";

            try {
                PayloadDTO dto = Mapper.fromJsonJackson(json, PayloadDTO.class);

                assertNotNull(dto);
                assertEquals(12345L, dto.getRegistrationNumber());
                assertEquals(1L, dto.getEntityType());
            } catch (MapperException e) {
                fail("Deserialization failed: " + e.getMessage());
            }
    }
}