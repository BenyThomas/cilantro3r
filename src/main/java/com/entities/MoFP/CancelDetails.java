package com.entities.MoFP;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@Getter
@Setter
@ToString
@XmlAccessorType(XmlAccessType.NONE)
public class CancelDetails {
    @XmlElement(name = "OrgMsgId", nillable = true)
    private String orgMsgId;
    @XmlElement(name = "PaymentType", nillable = true)
    private String paymentType;
    @XmlElement(name = "CreDtTm", nillable = true)
    private String creDtTm;
    @XmlElement(name = "Reason", nillable = true)
    private String reason;

}

