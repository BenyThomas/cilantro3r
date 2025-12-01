package com.entities;

import lombok.*;

@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PUBLIC)
@NoArgsConstructor
@ToString
public class Txn {
    private String amount;
    private String batch;
    private String channel;
    private String charge;
    private String chgId;
    private String crAcct;
    private String createDt;
    private String currency;
    private String drAcct;
    private String narration;
    private String recId;
    private String recSt;
    private String result;
    private String reverse;
    private String scheme;
    private String txnCd;
    private String txnId;
    private String txnRef;
    private String buId;
    private String code;
    private String millis;
    private String module;
    private String reqRef;
    private String tries;

    public String getBuId() {
        return buId;
    }

    public void setBuId(String buId) {
        this.buId = buId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMillis() {
        return millis;
    }

    public void setMillis(String millis) {
        this.millis = millis;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getReqRef() {
        return reqRef;
    }

    public void setReqRef(String reqRef) {
        this.reqRef = reqRef;
    }

    public String getTries() {
        return tries;
    }

    public void setTries(String tries) {
        this.tries = tries;
    }
}
