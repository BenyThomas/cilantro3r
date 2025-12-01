package com.entities.MoFP;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@ToString
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "MsgSummary")
public class StatementSummary {
    @XmlElement(name = "AcctName")
    private String acctName;
    @XmlElement(name = "AcctNum")
    private String acctNum;
    @XmlElement(name = "Currency")
    private String currency;
    @XmlElement(name = "CreDtTm")
    private Date creDtTm;
    @XmlElement(name = "SmtDt")
    private String smtDt;
    @XmlElement(name = "OpenCdtDbtInd")
    private String openCdtDbtInd;
    @XmlElement(name = "OpenBal")
    private BigDecimal openBal;
    @XmlElement(name = "CloseCdtDbtInd")
    private String CloseCdtDbtInd;
    @XmlElement(name = "CloseBal")
    private BigDecimal closeBal;
}
