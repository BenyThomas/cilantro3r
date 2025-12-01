/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.entities;

import java.util.Map;

/**
 *
 * @author MELLEJI
 */
public class ReportsJsonResponse2 {

    private boolean validated;
    private Object json;
    private Object json2;

    public Object getJson2() {
        return json2;
    }

    public void setJson2(Object json2) {
        this.json2 = json2;
    }
    
    private Map<String, String> errorMessages;

    public Object getJson() {
        return json;
    }

    public void setJson(Object json) {
        this.json = json;
    }

    public boolean isValidated() {
        return validated;
    }

    public void setValidated(boolean validated) {
        this.validated = validated;
    }

    public Map<String, String> getErrorMessages() {
        return errorMessages;
    }

    public void setErrorMessages(Map<String, String> errorMessages) {
        this.errorMessages = errorMessages;
    }

    @Override
    public String toString() {
        return "ReportsJsonResponse2{" + "validated=" + validated + ", json=" + json + ", errorMessages=" + errorMessages + '}';
    }

}
