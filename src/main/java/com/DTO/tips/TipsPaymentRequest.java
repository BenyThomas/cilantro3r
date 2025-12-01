package com.DTO.tips;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class TipsPaymentRequest {
    public String amount;
    public String benAccount;
    public String benInstitutionCategory;
    public String benInstitutionCode;
    public String beneficiaryName;
    public String channelCode;
    public String currency;
    public String description;
    public String msisdn;
    public String reference;
    public String senderAccount;
    public String senderName;
    public philae.api.UsRole userRole;
    public String callbackUrl;

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }

}
