package com.DTO.KYC.ors.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IssuedShareCapitalData {
    @JsonProperty("ordinary")
    private String ordinary;
    @JsonProperty("currency")
    private String currency;
    @JsonProperty("__INDEX")
    private String index;
    @JsonProperty("no_of_shares_issued")
    private int noOfSharesIssued;
    @JsonProperty("value")
    private double value;
    @JsonProperty("aggregate_nominal_value")
    private String aggregateNominalValue;


}
