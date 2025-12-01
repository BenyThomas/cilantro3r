package com.DTO.psssf;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PensionLookupReq {
    public String status;
    public String code;
    public ArrayList<Details> details;
    @Override
    public String toString() {
        Gson gson = new GsonBuilder().create();
        return gson.toJson(this);
    }
}
