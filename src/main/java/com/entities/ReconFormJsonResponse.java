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
public class ReconFormJsonResponse {

    private ReconForm reconForm;
    private boolean validated;
    private String jsonString;
    private Map<String, String> errorMessages;

    public String getJsonString() {
        return jsonString;
    }

    public void setJsonString(String jsonString) {
        this.jsonString = jsonString;
    }

    public ReconForm getReconForm() {
        return reconForm;
    }

    public void setReconForm(ReconForm reconForm) {
        this.reconForm = reconForm;
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

}
