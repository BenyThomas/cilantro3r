package com.DTO.stawi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StawiBondNotificationRequest {
    private String customerName;
    private String amount;        // keep as String if the provider expects stringified amounts
    private String currency;      // e.g. "TZS"
    private String dseAccount;    // CDS/DSE account number
    private String phoneNumber;   // e.g. "255657871769"
    private String narration;     // free text
    private String sourceAccount; // account debited on our side
    private String channel;       // "MOBILEAPP|IBANK|RTGS|EFT|TIPS"
    private String reference;
}
