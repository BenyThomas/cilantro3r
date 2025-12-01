package com.entities.MoFP;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.math.BigDecimal;

@Getter
@Setter
@ToString
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "TrxRecord")
public class StatementRecord {
    @XmlElement(name = "BankRef")
    private String bankRef;
    @XmlElement(name = "RelatedRef")
    private String relatedRef;
    @XmlElement(name = "TranType")
    private String tranType;
    @XmlElement(name = "TrxAmount")
    private BigDecimal trxAmount;
    @XmlElement(name = "ExchangeRate")
    private String exchangeRate;
    @XmlElement(name = "TranCode")
    private String tranCode;
    @XmlElement(name = "Description")
    private String description;
}
