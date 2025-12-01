/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.IBANK;

/**
 *
 * @author melleji.mollel Feb 17, 2021 8:12:08 PM
 */
public class Customer {

    private String name;
    private String custId;
    private String payload;
    private String payloadType;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCustId() {
        return custId;
    }

    public void setCustId(String custId) {
        this.custId = custId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getPayloadType() {
        return payloadType;
    }

    public void setPayloadType(String payloadType) {
        this.payloadType = payloadType;
    }

    @Override
    public String toString() {
        return "Customer{" + "name=" + name + ", custId=" + custId + ", payload=" + payload + ", payloadType=" + payloadType + '}';
    }

}
