package com.dao.sms;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SMSResponse {
    @JsonProperty("STATUS")
    private String status;
    @JsonProperty("DESCRIPTION")
    private String description;
}