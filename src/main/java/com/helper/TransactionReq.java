package com.helper;


import lombok.Getter;
import lombok.Setter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@Getter
@Setter
@XmlRootElement(name = "Request")
@XmlAccessorType(XmlAccessType.FIELD)
public class TransactionReq {
    private String reference;
    private String transferType;
}
