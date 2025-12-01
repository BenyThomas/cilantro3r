package com.DTO.salary;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "PAYROLL")
@Getter
@Setter
@ToString
public class PAYROLL {
    @XmlElement(name = "PAYROLL_SUMMARY", required = true)
    private PAYROLL_SUMMARY PAYROLL_SUMMARY;
    @XmlElement(name = "TRANSACTIONS", required = true)
    private TRANSACTIONS TRANSACTIONS;


}
