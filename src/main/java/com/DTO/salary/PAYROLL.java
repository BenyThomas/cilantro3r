package com.DTO.salary;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

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
