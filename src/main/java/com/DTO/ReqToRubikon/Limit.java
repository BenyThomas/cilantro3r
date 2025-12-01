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

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;


@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "limit", propOrder ={
    "creditLimit",
    "currency",
    "debitLimit",
    "role",
    "roleId"
})
@Getter
@Setter

public class Limit {

   @XmlElement(name = "creditLimit")
    public String creditLimit;
   @XmlElement(name = "currency")
    public String currency;
   @XmlElement(name = "debitLimit")
    public String debitLimit;
   @XmlElement(name = "role")
    public String role;
   @XmlElement(name = "roleId")
    public String roleId;
     @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
 
}
