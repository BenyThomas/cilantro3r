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
public class AcctRecord {
    @XmlElement(name = "EndToEndId")
    private String endToEndId;
    @XmlElement(name = "EndtoEndId")
    private String endtoEndId;
    @XmlElement(name = "OrgEndtoEndId")
    private String orgEndToEndId;
    @XmlElement(name = "OldName")
    private String oldName;
    @XmlElement(name = "NewName")
    private String newName;
    @XmlElement(name = "Name")
    private String name;
    @XmlElement(name = "Ccy", nillable = true)
    private String ccy;
    @XmlElement(name = "TransBic")
    private String transBic;
    @XmlElement(name = "TransAcctNum")
    private String transAcctNum;
    @XmlElement(name = "TransAcctName")
    private String transAcctName;
    @XmlElement(name = "TransCcy")
    private String transCcy;
    @XmlElement(name = "TransferredAmt")
    private String transferredAmt;
    @XmlElement(name = "ChangeDtTm")
    private String changeDtTm;
    @XmlElement(name = "ChangingStatus")
    private String changingStatus;
    @XmlElement(name = "ChangingStatusDesc")
    private String changingStatusDesc;
    @XmlElement(name = "CloseDtTm")
    private String closeDtTm;
    @XmlElement(name = "ClosingStatus")
    private String closingStatus;
    @XmlElement(name = "ClosingStatusDesc")
    private String closingStatusDesc;
    @XmlElement(name = "Category")
    private String category;
    @XmlElement(name = "AcctType")
    private String acctType;
    @XmlElement(name = "AcctNum")
    private String acctNum;
    @XmlElement(name = "ClosedAcctNum")
    private String closedAcctNum;
    @XmlElement(name = "BranchName")
    private String branchName;
    @XmlElement(name = "BranchCode")
    private String branchCode;
    @XmlElement(name = "Operator")
    private String operator;
    @XmlElement(name = "Owner")
    private String owner;
    @XmlElement(name = "OperatorCat")
    private String operatorCat;
    @XmlElement(name = "Purpose")
    private String purpose;
    @XmlElement(name = "OtherDetails")
    private String otherDetails;
    @XmlElement(name = "RegionCode")
    private String regionCode;
    @XmlElement(name = "District")
    private String district;
    @XmlElement(name = "PhoneNum")
    private String phoneNum;
    @XmlElement(name = "Email")
    private String email;
    @XmlElement(name = "PostalAddr")
    private String postalAddr;
    @XmlElement(name = "OpenDt")
    private String openDt;
    @XmlElement(name = "OpeningStatus")
    private String openingStatus;
    @XmlElement(name = "OpeningStatusDesc")
    private String openingStatusDesc;

}
