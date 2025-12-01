/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.DTO.recon;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 *
 * @author daudi.kajilo
 */
@Getter
@Setter
public class LedgerReconDataReq {
private String reference;
private String txnType;
private String drcrInd;
private String narration;
private BigDecimal amount;
private String sourceAcct;
private String benAccount;
private String transDate;
private String exceptionType;
private BigDecimal closingBalance;
private String status;

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
}
