/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.EFT;

/**
 *
 * @author melleji.mollel
 */
public class EftBulkPaymentResp {

    public String statusCode;
    public String statusMessage;

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    @Override
    public String toString() {
        return "EftBulkPaymentResp{" + "statusCode=" + statusCode + ", statusMessage=" + statusMessage + '}';
    }
    
    

}
