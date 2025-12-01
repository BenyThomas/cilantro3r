package com.DTO.tausi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeName("root")
public class AgentTransactionSettlement {
    @JsonProperty("agentCode")
    private String agentCode;
    @JsonProperty("agentReference")
    private String agentReference;
    @JsonProperty("spCode")
    private String spCode;
    @JsonProperty("verifiedEgaPartner")
    private String verifiedEgaPartner;
    @JsonProperty("destinationAcct")
    private String destinationAcct;
    @JsonProperty("destinationAcctName")
    private String destinationAcctName;
    @JsonProperty("amount")
    private int amount;
    @JsonProperty("currency")
    private String currency;
    @JsonProperty("transactionId")
    private String transactionId;
    @JsonProperty("requestTime")
    private String  requestTime;

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
}