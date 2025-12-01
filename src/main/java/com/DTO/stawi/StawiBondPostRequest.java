package com.DTO.stawi;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StawiBondPostRequest {
    private String transactionReference;
    private String dseAccount;
    private String reason;
}
