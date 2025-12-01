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
@JsonTypeName("data")
public class AgentDepositNotifyDataResp {
    @JsonProperty("agentCode")
    private String agentCode;
    @JsonProperty("agentReference")
    private String agentReference;
    @JsonProperty("amount")
    private double amount;
    @JsonProperty("transactionId")
    private String transactionId;
    @JsonProperty("requestTime")
    private String requestTime;

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
}
