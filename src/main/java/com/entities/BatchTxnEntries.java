/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author melleji.mollel
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BatchTxnEntries {

    private String buId;
    private BigDecimal amount;
    private String batch;
    private String module;
    private String crAcct;
    private String currency;
    private String drAcct;
    private String narration;
    private String reverse;
    private String scheme;
    private String code;
    private String txnRef;
      @Override
    public String toString() {
        Gson gson = new GsonBuilder().create();
        return gson.toJson(this);
    }
}
