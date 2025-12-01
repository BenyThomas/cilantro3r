/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.IBANK;

import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;

/**
 *
 * @author Dell
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class CustomersPaymentReq {

    
    public List<PaymentBatchEntriesReq> paymentRequest;

    public List<PaymentBatchEntriesReq> getPaymentRequest() {
        return paymentRequest;
    }

    
    public void setPaymentRequest(List<PaymentBatchEntriesReq> paymentRequest) {
        this.paymentRequest = paymentRequest;
    }

    @Override
    public String toString() {
        return "CustomersPaymentReq{" + "paymentRequest=" + paymentRequest + '}';
    }

}
