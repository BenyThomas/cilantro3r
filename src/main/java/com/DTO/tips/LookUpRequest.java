package com.DTO.tips;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LookUpRequest {
    private String accountNo;
    private String currency;
    private String institutionCategory;
    private  String institutionCode;
    private  String reference;

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
}
