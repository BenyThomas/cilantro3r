/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.ReqToRubikon;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "drawer", propOrder = {
    "currencies",
    "drawerAccount",
    "drawerId",
    "drawerNumber",
    "openDate",
    "status"})
@Getter
@Setter
@ToString
public class Drawer {

   @JacksonXmlProperty(localName = "currencies")
    public Currencies currencies;
   @JacksonXmlProperty(localName = "drawerAccount")
    public String drawerAccount;
   @JacksonXmlProperty(localName = "drawerId")
    public String drawerId;
   @JacksonXmlProperty(localName = "drawerNumber")
    public String drawerNumber;
   @JacksonXmlProperty(localName = "openDate")
    public String openDate;
   @JacksonXmlProperty(localName = "status")
    public String status;
 @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
   
}
