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
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


@XmlRootElement(name = "request")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "request", propOrder = {
    "reference",
    "debitAccount",
    "creditAccount",
    "currency",
    "amount",
    "debitFxRate",
    "creditFxRate",
    "narration",
    "scheme",
    "userRole",
    "reversal",
    "drawer"

})
@Getter
@Setter
public class Request {

    @XmlElement(name = "reference")
    public String reference;
    @XmlElement(name = "debitAccount")
    public String debitAccount;
    @XmlElement(name = "creditAccount")
    public String creditAccount;
    @XmlElement(name = "currency")
    public String currency;
    @XmlElement(name = "amount")
    public String amount;
    @XmlElement(name = "debitFxRate")
    public String debitFxRate;
    @XmlElement(name = "creditFxRate")
    public String creditFxRate;
    @XmlElement(name = "narration")
    public String narration;
    @XmlElement(name = "scheme")
    public String scheme;
    @XmlElement(name = "userRole")
    public UserRole userRole;
    @XmlElement(name = "reversal")
    public String reversal;
    @XmlElement(name = "drawer")
    public Drawer drawer;

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().create();
        return gson.toJson(this);
    }
}
