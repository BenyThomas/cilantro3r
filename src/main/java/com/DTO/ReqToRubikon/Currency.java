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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "currency", propOrder = {
    "code",
    "id",
    "name",
    "points",})
@Getter
@Setter
@ToString
public class Currency {

    @JacksonXmlProperty(localName = "code")
    public String code;
    @JacksonXmlProperty(localName = "id")
    public String id;
    @JacksonXmlProperty(localName = "name")
    public String name;
    @JacksonXmlProperty(localName = "points")
    public String points;

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
}
