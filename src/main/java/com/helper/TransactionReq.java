package com.helper;


import lombok.Getter;
import lombok.Setter;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

@Getter
@Setter
@XmlRootElement(name = "Request")
@XmlAccessorType(XmlAccessType.FIELD)
public class TransactionReq {
    private String reference;
    private String transferType;
}
