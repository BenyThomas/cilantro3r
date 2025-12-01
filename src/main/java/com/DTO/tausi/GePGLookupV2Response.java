package com.DTO.tausi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class GePGLookupV2Response {
    private String spCode;
    private String spName;
    private String billPayOption;
    private String destinationAcct;
    private String egaPartnerName;
    private String egaPartnerAcctNo;
    private String egaPartnerSpCode;

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
}
