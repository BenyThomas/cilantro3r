/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.IBANK;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author melleji.mollel
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "name",
    "swiftCode",
        "tipsBankCode",
        "fspCategory",
        "fspStatus",
    "identifier"
})
public class Banks {

    public String name;
    public String swiftCode;
    public String identifier;
    public String tipsBankCode;
    public String fspCategory;
    public String fspStatus;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSwiftCode() {
        return swiftCode;
    }

    public void setSwiftCode(String swiftCode) {
        this.swiftCode = swiftCode;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getTipsBankCode() {
        return tipsBankCode;
    }

    public void setTipsBankCode(String tipsBankCode) {
        this.tipsBankCode = tipsBankCode;
    }

    public String getFspCategory() {
        return fspCategory;
    }

    public void setFspCategory(String fspCategory) {
        this.fspCategory = fspCategory;
    }

    public String getFspStatus() {
        return fspStatus;
    }

    public void setFspStatus(String fspStatus) {
        this.fspStatus = fspStatus;
    }

    @Override
    public String toString() {
        return "Banks{" +
                "name='" + name + '\'' +
                ", swiftCode='" + swiftCode + '\'' +
                ", identifier='" + identifier + '\'' +
                ", tipsBankCode='" + tipsBankCode + '\'' +
                ", fspCategory='" + fspCategory + '\'' +
                ", fspStatus='" + fspStatus + '\'' +
                '}';
    }
}
