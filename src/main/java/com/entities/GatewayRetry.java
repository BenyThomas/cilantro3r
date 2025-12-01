/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.entities;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author MELLEJI
 */
@XmlRootElement(name = "CBS_RETRY")
public class GatewayRetry {

    @XmlElement(name = "msisdn")
    private String msisdn;
    @XmlElement(name = "imsi")
    private String imsi;
    @XmlElement(name = "account")
    private String account;
    @XmlElement(name = "toaccount")
    private String toaccount;
    @XmlElement(name = "amount")
    private String amount;
    @XmlElement(name = "trans_type")
    private String trans_type;
    @XmlElement(name = "processcode")
    private String processcode;
    @XmlElement(name = "msgid")
    private String msgid;

    public GatewayRetry() {

    }

    public GatewayRetry(String msisdn, String imsi, String account, String toaccount, String amount, String trans_type, String processcode, String msgid) {
        this.msisdn = msisdn;
        this.imsi = imsi;
        this.account = account;
        this.toaccount = toaccount;
        this.amount = amount;
        this.trans_type = trans_type;
        this.processcode = processcode;
        this.msgid = msgid;
    }

    @Override
    public String toString() {
        return "GatewayRetry{" + "msisdn=" + msisdn + ", imsi=" + imsi + ", account=" + account + ", toaccount=" + toaccount + ", amount=" + amount + ", trans_type=" + trans_type + ", processcode=" + processcode + ", msgid=" + msgid + '}';
    }

}
