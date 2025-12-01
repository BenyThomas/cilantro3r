package com.entities.MoFP;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@Getter
@Setter
@ToString
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "Document")
public class Document {
    @XmlElement(name = "Header")
    private Header Header;
    @XmlElement(name = "MsgSummary")
    private MsgSummary MsgSummary;
    @XmlElement(name = "MsgSummary")
    private StatementSummary StatementSummary;
    @XmlElement(name = "Details")
    private Details Details;
    @XmlElement(name = "Attachments")
    private Attachments Attachments;
    @XmlElement(name = "CancelDetails")
    private CancelDetails CancelDetails;
    @XmlElement(name = "RequestSummary")
    private RequestSummary RequestSummary;
    @XmlElement(name = "TrxRecord")
    private List<TrxRecord> TrxRecords;
    @XmlElement(name = "TrxRecord")
    private List<StatementRecord> StatementRecords;
}

