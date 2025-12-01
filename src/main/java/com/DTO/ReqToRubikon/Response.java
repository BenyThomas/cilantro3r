/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.ReqToRubikon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.math.BigDecimal;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "response", propOrder = {
    "result",
    "reference",
    "message",
    "availableBalance",
    "ledgerBalance",
    "txnId"
})
@Getter
@Setter
public class Response implements Serializable {

    protected int result;
    @XmlElement(required = true, nillable = true)
    protected String reference;
    @XmlElement(required = true, nillable = true)
    protected String message;
    @XmlElement(required = true, nillable = true)
    protected BigDecimal availableBalance;
    @XmlElement(required = true, nillable = true)
    protected BigDecimal ledgerBalance;
    @XmlElement(required = true, nillable = true)
    protected String txnId;
     @Override
    public String toString() {
        Gson gson = new GsonBuilder().create();
        return gson.toJson(this);
    }
}
