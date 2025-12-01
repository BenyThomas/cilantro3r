/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.entities.MoFP;

/**
 *
 * @author samichael
 */
public class JwtResponse {
    private String token;
    private String status;
    private String message;
    private String forcePasswordChange;
    private String passwordExpiryDate;
    public JwtResponse(String status, String message, String token, String forcePasswordChange, String passwordExpiryDate) {
        this.status = status;
        this.message = message;
        this.token = token;
        this.forcePasswordChange = forcePasswordChange;
        this.passwordExpiryDate = passwordExpiryDate;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getForcePasswordChange() {
        return forcePasswordChange;
    }

    public void setForcePasswordChange(String forcePasswordChange) {
        this.forcePasswordChange = forcePasswordChange;
    }

    public String getPasswordExpiryDate() {
        return passwordExpiryDate;
    }

    public void setPasswordExpiryDate(String passwordExpiryDate) {
        this.passwordExpiryDate = passwordExpiryDate;
    }
 
}
