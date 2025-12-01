package com.DTO.tips;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Identity {
    public String type;
    public String value;

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
}
