package com.DTO.ubx;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LinkedAccount {

    @JsonProperty("accountType")
    private String accountType;

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("indicator")
    private String indicator;

    // Getters and Setters
}
