package com.entities.MoFP;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Getter
@Setter
@ToString
@XmlAccessorType(XmlAccessType.NONE)
public class RequestSummary {
    @XmlElement(name = "RequestId")
    private String requestId;
    @XmlElement(name = "CreDtTm")
    private String creDtTm;
    @XmlElement(name = "AcctNum")
    private String accountNum;
}

