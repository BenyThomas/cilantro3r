package com.DTO.psssf;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PensionerLoanVerificationResponse {
    private String reference;
    private String comments;
    private  String status;
    private String responseCode;
    private String approver;

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
}
