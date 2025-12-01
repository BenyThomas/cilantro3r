package com.entities.MoFP;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.List;

@Getter
@Setter
@ToString
@XmlAccessorType(XmlAccessType.NONE)
public class Details {
    @XmlElement(name = "AcctRecord")
    private List<AcctRecord> AcctRecord;
}
