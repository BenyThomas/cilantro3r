package com.entities.MoFP;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import java.math.BigDecimal;

@Getter
@Setter
@ToString
@XmlAccessorType(XmlAccessType.NONE)
public class TrxRecord {
    @XmlElement(name = "AcctNum")
    private String acctNum;
    @XmlElement(name = "Ccy")
    private String currency;
    @XmlElement(name = "AcctStatus")
    private String accountStatus;
    @XmlElement(name = "BalCdtDbtInd")
    private String balCdtDbtInd;
    @XmlElement(name = "AcctBal")
    private BigDecimal balance;
    @XmlElement(name = "NbOfRecTiss")
    private Integer nbOfRecTiss;
    @XmlElement(name = "NbOfRecSwift")
    private Integer nbOfRecSwift;
    @XmlElement(name = "NbOfRecClearance")
    private Integer nbOfRecClearance;
    @XmlElement(name = "NbOfRecCash")
    private Integer nbOfRecCash;
    @XmlElement(name = "NbOfRecEft")
    private Integer nbOfRecEft;
    @XmlElement(name = "NbOfRecTips")
    private Integer nbOfRecTips;
    @XmlElement(name = "NbOfRecWallet")
    private Integer nbOfRecWallet;
    @XmlElement(name = "NbOfRecInternalTransfer")
    private Integer nbOfRecInternalTransfer;
}
