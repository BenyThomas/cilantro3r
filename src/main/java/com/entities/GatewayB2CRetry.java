/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.entities;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author MELLEJI
 */
@XmlRootElement(name = "B2C_RETRY")
public class GatewayB2CRetry {

    @XmlElement(name = "referNumber")
    private String referNumber;
    @XmlElement(name = "msisdn")
    private String msisdn;
    @XmlElement(name = "account")
    private String account;
    @XmlElement(name = "toaccount")
    private String toaccount;
    @XmlElement(name = "amount")
    private String amount;

    public GatewayB2CRetry() {

    }

    public GatewayB2CRetry(String referNumber, String msisdn, String account, String toaccount, String amount) {
        this.referNumber = referNumber;
        this.msisdn = msisdn;
        this.account = account;
        this.toaccount = toaccount;
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "GatewayB2CRetry{" + "referNumber=" + referNumber + ", msisdn=" + msisdn + ", account=" + account + ", toaccount=" + toaccount + ", amount=" + amount + '}';
    }

}
