package com.DTO.salary;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "PAYROLL_SUMMARY")
public class PAYROLL_SUMMARY {
    @XmlElement(name = "NO_OF_EMPLOYEES", required = true)
    private String NO_OF_EMPLOYEES;
    @XmlElement(name = "PAYROLL_AMOUNT", required = true)
    private String PAYROLL_AMOUNT;
    @XmlElement(name = "PAYROLL_DATE", required = true)
    private String PAYROLL_DATE;
    @XmlElement(name = "MONTH", required = true)
    private String MONTH;
    @XmlElement(name = "YEAR", required = true)
    private String YEAR;
    @XmlElement(name = "INITIATOR", required = true)
    private String INITIATOR;
    @XmlElement(name = "DESCRIPTION", required = true)
    private String DESCRIPTION;

    public String getNO_OF_EMPLOYEES() {
        return NO_OF_EMPLOYEES;
    }

    public void setNO_OF_EMPLOYEES(String NO_OF_EMPLOYEES) {
        this.NO_OF_EMPLOYEES = NO_OF_EMPLOYEES;
    }

    public String getPAYROLL_AMOUNT() {
        return PAYROLL_AMOUNT;
    }

    public void setPAYROLL_AMOUNT(String PAYROLL_AMOUNT) {
        this.PAYROLL_AMOUNT = PAYROLL_AMOUNT;
    }

    public String getPAYROLL_DATE() {
        return PAYROLL_DATE;
    }

    public void setPAYROLL_DATE(String PAYROLL_DATE) {
        this.PAYROLL_DATE = PAYROLL_DATE;
    }

    public String getMONTH() {
        return MONTH;
    }

    public void setMONTH(String MONTH) {
        this.MONTH = MONTH;
    }

    public String getYEAR() {
        return YEAR;
    }

    public void setYEAR(String YEAR) {
        this.YEAR = YEAR;
    }

    public String getINITIATOR() {
        return INITIATOR;
    }

    public void setINITIATOR(String INITIATOR) {
        this.INITIATOR = INITIATOR;
    }

    public String getDESCRIPTION() {
        return DESCRIPTION;
    }

    public void setDESCRIPTION(String DESCRIPTION) {
        this.DESCRIPTION = DESCRIPTION;
    }

    @Override
    public String toString() {
        return "PAYROLL_SUMMARY{" +
                "NO_OF_EMPLOYEES='" + NO_OF_EMPLOYEES + '\'' +
                ", PAYROLL_AMOUNT='" + PAYROLL_AMOUNT + '\'' +
                ", PAYROLL_DATE='" + PAYROLL_DATE + '\'' +
                ", MONTH='" + MONTH + '\'' +
                ", YEAR='" + YEAR + '\'' +
                ", INITIATOR='" + INITIATOR + '\'' +
                ", DESCRIPTION='" + DESCRIPTION + '\'' +
                '}';
    }
}
