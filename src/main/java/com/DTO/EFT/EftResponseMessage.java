/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.EFT;

import java.util.Map;

/**
 *
 * @author melleji.mollel
 */
public class EftResponseMessage {

    private String message;
    private boolean validated;
    private String jsonString;
    private Map<String, String> errorMessages;

    public EftResponseMessage(String message) {
        this.message = message;
    }

    public EftResponseMessage(String message, boolean validated, String jsonString, Map<String, String> errorMessages) {
        this.message = message;
        this.validated = validated;
        this.jsonString = jsonString;
        this.errorMessages = errorMessages;
    }

  

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isValidated() {
        return validated;
    }

    public void setValidated(boolean validated) {
        this.validated = validated;
    }

    public String getJsonString() {
        return jsonString;
    }

    public void setJsonString(String jsonString) {
        this.jsonString = jsonString;
    }

    public Map<String, String> getErrorMessages() {
        return errorMessages;
    }

    public void setErrorMessages(Map<String, String> errorMessages) {
        this.errorMessages = errorMessages;
    }

    @Override
    public String toString() {
        return "EftResponseMessage{" + "message=" + message + ", validated=" + validated + ", jsonString=" + jsonString + ", errorMessages=" + errorMessages + '}';
    }

}
