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
public class AgentDepositNotifyRootResp {
    @JsonProperty("status")
    private boolean status;
    @JsonProperty("statusCode")
    private int statusCode;
    @JsonProperty("description")
    private String description;
    @JsonProperty("data")
    private AgentDepositNotifyDataResp data;
    @JsonProperty("signature")
    private String signature;

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
}
