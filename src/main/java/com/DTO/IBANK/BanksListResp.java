/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.IBANK;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author melleji.mollel
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "bankType",
    "bank"
})
@XmlRootElement(name = "bankListResponse")
public class BanksListResp {

    public String bankType;
    public List<Banks> bank;

    public String getBankType() {
        return bankType;
    }

    public void setBankType(String bankType) {
        this.bankType = bankType;
    }

    public List<Banks> getBank() {
        return bank;
    }

    public void setBank(List<Banks> bank) {
        this.bank = bank;
    }

    @Override
    public String toString() {
        return "BanksListResp{" + "bankType=" + bankType + ", bank=" + bank + '}';
    }

}
