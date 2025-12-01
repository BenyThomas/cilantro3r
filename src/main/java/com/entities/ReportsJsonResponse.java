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
public class ReportsJsonResponse {

    private ReportsForm reportsForm;
    private boolean validated;
    private String jsonString;
    private Map<String, String> errorMessages;

    public String getJsonString() {
        return jsonString;
    }

    public void setJsonString(String jsonString) {
        this.jsonString = jsonString;
    }

    public ReportsForm getReportsForm() {
        return reportsForm;
    }

    public void setReportsForm(ReportsForm reportsForm) {
        this.reportsForm = reportsForm;
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
