package com.DTO.tips;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TipsPaymentResponse {
    private String responseCode;
    private String message;
    private  String account;
    private String reference;
    private String bankReference;

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
}
