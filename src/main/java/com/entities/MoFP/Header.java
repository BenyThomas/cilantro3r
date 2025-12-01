package com.entities.MoFP;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@Getter
@Setter
@ToString
@XmlAccessorType(XmlAccessType.NONE)
public class Header {
    @XmlElement(name = "Sender")
    private String sender;
    @XmlElement(name = "Receiver")
    private String receiver;
    @XmlElement(name = "MsgId")
    private String msgId;
    @XmlElement(name = "PaymentType")
    private String paymentType;
    @XmlElement(name = "MessageType")
    private String messageType;
}
