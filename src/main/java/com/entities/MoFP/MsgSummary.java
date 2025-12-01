package com.entities.MoFP;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.Date;

@Getter
@Setter
@ToString
@XmlAccessorType(XmlAccessType.NONE)
public class MsgSummary {
    @XmlElement(name = "Bic")
    private String bic;
    @XmlElement(name = "AuthorityRef")
    private String authorityRef;
    @XmlElement(name = "BotRef")
    private String botRef;
    @XmlElement(name = "Ref")
    private String ref;
    @XmlElement(name = "CreDtTm")
    private Date creDtTm;
    @XmlElement(name = "ExprDt")
    private Date exprDt;
    @XmlElement(name = "NbOfRec")
    private int nbOfRec;
    @XmlElement(name = "OrgRequestId")
    private String orgRequestId;
}
