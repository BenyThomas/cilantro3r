package com.entities;

import java.util.ArrayList;
import java.util.List;

public class ODPensionersResp {
    private String reponseCode;
    private String callbackUrl1;
    private String message;
    private List<OutstandingLoan> outstandingLoans = new ArrayList<>();
    public String getReponseCode() {
        return reponseCode;
    }
    public void setReponseCode(String reponseCode) {
        this.reponseCode = reponseCode;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public List<OutstandingLoan> getOutstandingLoans() {
        return outstandingLoans;
    }
    public void setOutstandingLoans(List<OutstandingLoan> outstandingLoans) {
        this.outstandingLoans = outstandingLoans;
    }
    public String getCallbackUrl1() {
        return callbackUrl1;
    }
    public void setCallbackUrl1(String callbackUrl) {
        this.callbackUrl1 = callbackUrl;
    }

}
