/*
 * To change this license header; public choose License Headers in Project Properties.
 * To change this template file; public choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.IBANK;

import java.math.BigDecimal;
import javax.persistence.Column;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 *
 * @author melleji.mollel #
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "reference",
        "senderAccount",
        "batchReference",
        "senderName",
        "senderPhone",
        "senderAddress",
        "senderBic",
        "correspondentBic",
        "amount",
        "currency",
        "beneficiaryAccount",
        "beneficiaryName",
        "beneficiaryBIC",
        "beneficiaryContact",
        "description",
        "intermediaryBank",
        "specialRateToken",
        "initiatorId",
        "chargeCategory",
        "type",
        "isGepg",
        "customerType",
        "relatedReference",
        "customerBranch",
        "boundary",
        "callbackUrl",
        "spRate",
        "createDt"
})
@XmlRootElement(name = "paymentRequest")
@Getter
@Setter
@ToString
public class PaymentReq {

    @XmlElement(name = "reference", required = true)
    private String reference;
    @XmlElement(name = "senderAccount", required = true)
    private String senderAccount;
    @XmlElement(name = "senderName", required = true)
    private String senderName;
    @XmlElement(name = "amount", required = true)
    private BigDecimal amount;
    @XmlElement(name = "currency", required = true)
    private String currency;
    @XmlElement(name = "beneficiaryAccount", required = true)
    private String beneficiaryAccount;
    @XmlElement(name = "beneficiaryName", required = true)
    private String beneficiaryName;
    @XmlElement(name = "beneficiaryBIC", required = true)
    private String beneficiaryBIC;
    @XmlElement(name = "senderAddress", required = true)
    private String senderAddress;
    @XmlElement(name = "beneficiaryContact", required = true)
    private String beneficiaryContact;
    @XmlElement(name = "senderPhone", required = true)
    private String senderPhone;
    @XmlElement(name = "description", required = true)
    private String description;
    @XmlElement(name = "intermediaryBank", required = true)
    private String intermediaryBank;
    @XmlElement(name = "specialRateToken", required = true)
    private String specialRateToken;
    @XmlElement(name = "initiatorId", required = true)
    private String initiatorId;
    @XmlElement(name = "chargeCategory", required = true)
    private String chargeCategory;
    @XmlElement(name = "type", required = true)
    private String type;//is EFT/RTGS/
    @XmlElement(name = "boundary", required = true)
    private String boundary;
    @XmlElement(name = "isGepg", required = false, nillable = true)
    private String isGepg;
    @XmlElement(name = "callbackUrl", required = true)
    private String callbackUrl;
    @XmlElement(name = "customerBranch", required = true)
    private String customerBranch;
    @XmlElement(name = "batchReference", required = true)
    private String batchReference;
    @XmlElement(name = "customerType", required = false)
    private String customerType;
    @XmlElement(name = "relatedReference", required = false)
    private String relatedReference;
    @XmlElement(name = "senderBic", required = false)
    private String senderBic;
    @XmlElement(name = "correspondentBic", required = false)
    private String correspondentBic;
    @XmlElement(name = "spRate", required = false,defaultValue ="0")
    private String spRate;

//    @Column(name = "create_dt")
//    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @XmlElement(name = "create_dt", required = false)
    private String createDt;
    
}
