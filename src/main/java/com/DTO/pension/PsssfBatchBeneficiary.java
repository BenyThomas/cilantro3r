/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.pension;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author melleji.mollel
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PsssfBatchBeneficiary {

    @JsonProperty("ID")
    private String ID;

    @JsonProperty("NAME")
    private String NAME;

    @JsonProperty("CURRENCY")
    private String CURRENCY;

    @JsonProperty("AMOUNT")
    private String AMOUNT;

    @JsonProperty("ACCOUNT")
    private String ACCOUNT;

    @JsonProperty("CHANNELIDENTIFIER")
    private String CHANNELIDENTIFIER;

    @JsonProperty("DESTINATIONCODE")
    private String DESTINATIONCODE;

    @JsonProperty("PENSIONER_ID")
    private String PENSIONER_ID;

    @JsonProperty("NARRATION")
    private String NARRATION;

    @JsonProperty("REASON")
    private String REASON;

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public String getNAME() {
        return NAME;
    }

    public void setNAME(String NAME) {
        this.NAME = NAME;
    }

    public String getCURRENCY() {
        return CURRENCY;
    }

    public void setCURRENCY(String CURRENCY) {
        this.CURRENCY = CURRENCY;
    }

    public String getAMOUNT() {
        return AMOUNT;
    }

    public void setAMOUNT(String AMOUNT) {
        this.AMOUNT = AMOUNT;
    }

    public String getACCOUNT() {
        return ACCOUNT;
    }

    public void setACCOUNT(String ACCOUNT) {
        this.ACCOUNT = ACCOUNT;
    }

    public String getCHANNELIDENTIFIER() {
        return CHANNELIDENTIFIER;
    }

    public void setCHANNELIDENTIFIER(String CHANNELIDENTIFIER) {
        this.CHANNELIDENTIFIER = CHANNELIDENTIFIER;
    }

    public String getDESTINATIONCODE() {
        return DESTINATIONCODE;
    }

    public void setDESTINATIONCODE(String DESTINATIONCODE) {
        this.DESTINATIONCODE = DESTINATIONCODE;
    }

    public String getPENSIONER_ID() {
        return PENSIONER_ID;
    }

    public void setPENSIONER_ID(String PENSIONER_ID) {
        this.PENSIONER_ID = PENSIONER_ID;
    }

    public String getNARRATION() {
        return NARRATION;
    }

    public void setNARRATION(String NARRATION) {
        this.NARRATION = NARRATION;
    }

    @Override
    public String toString() {
        return "PsssfBatchBeneficiary{" + "ID=" + ID + ", NAME=" + NAME + ", CURRENCY=" + CURRENCY + ", AMOUNT=" + AMOUNT + ", ACCOUNT=" + ACCOUNT + ", CHANNELIDENTIFIER=" + CHANNELIDENTIFIER + ", DESTINATIONCODE=" + DESTINATIONCODE + ", PENSIONER_ID=" + PENSIONER_ID + ", NARRATION=" + NARRATION + '}';
    }

    public String getREASON() {
        return REASON;
    }

    public void setREASON(String REASON) {
        this.REASON = REASON;
    }
}
