package com.DTO.stawi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StawiBondLookupRequest {
    @JsonProperty("dseAccount")
    private String dseAccount;
    private String transactionReference;
}
