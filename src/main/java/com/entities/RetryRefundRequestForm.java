/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.entities;

import java.util.List;
import javax.validation.constraints.NotBlank;

/**
 *
 * @author MELLEJI
 */
public class RetryRefundRequestForm {
   
    @NotBlank
    private String reason;

    private List<RetryRefundRequest> retryRefundRequest;

    public List<RetryRefundRequest> getRetryRefundRequest() {
        return retryRefundRequest;
    }

    public void setRetryRefundRequest(List<RetryRefundRequest> retryRefundRequest) {
        this.retryRefundRequest = retryRefundRequest;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
      
    @Override
    public String toString() {
        return "RetryRefundRequestForm{" + "retryRefundRequest=" + retryRefundRequest + '}';
    }    

}
