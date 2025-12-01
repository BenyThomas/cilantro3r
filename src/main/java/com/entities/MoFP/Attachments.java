package com.entities.MoFP;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import java.util.List;

@Getter
@Setter
@ToString
@XmlAccessorType(XmlAccessType.NONE)
public class Attachments {

    @XmlElement(name = "Attachment1", nillable = true)
    private String Attachment1;

    @XmlElement(name = "Attachment2", nillable = true)
    private String Attachment2;

    @XmlElement(name = "Attachment3", nillable = true)
    private String Attachment3;
}
