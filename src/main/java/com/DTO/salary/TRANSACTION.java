package com.DTO.salary;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "TRANSACTION")
public class TRANSACTION {
    @XmlElement(name = "PF_NO", required = true)
    public String PF_NO;
    @XmlElement(name = "NAME", required = true)
    public String NAME;
    @XmlElement(name = "ACCOUNT", required = true)
    public String ACCOUNT;
    @XmlElement(name = "REFERENCE", required = true)
    public String REFERENCE;
    @XmlElement(name = "CREDIT_TO_DQA", required = true)
    public String CREDIT_TO_DQA;
    @XmlElement(name = "BRANCH_NO", required = true)
    public String BRANCHNO;
    @XmlElement(name = "PAYER_ACCT", required = true)
    public String PAYER_ACCT;

    public String getPF_NO() {
        return PF_NO;
    }

    public void setPF_NO(String PF_NO) {
        this.PF_NO = PF_NO;
    }

    public String getNAME() {
        return NAME;
    }

    public void setNAME(String NAME) {
        this.NAME = NAME;
    }

    public String getACCOUNT() {
        return ACCOUNT;
    }

    public void setACCOUNT(String ACCOUNT) {
        this.ACCOUNT = ACCOUNT;
    }

    public String getREFERENCE() {
        return REFERENCE;
    }

    public void setREFERENCE(String REFERENCE) {
        this.REFERENCE = REFERENCE;
    }

    public String getCREDIT_TO_DQA() {
        return CREDIT_TO_DQA;
    }

    public void setCREDIT_TO_DQA(String CREDIT_TO_DQA) {
        this.CREDIT_TO_DQA = CREDIT_TO_DQA;
    }

    public String getBRANCHNO() {
        return BRANCHNO;
    }

    public void setBRANCHNO(String BRANCHNO) {
        this.BRANCHNO = BRANCHNO;
    }

    public String getPAYER_ACCT() {
        return PAYER_ACCT;
    }

    public void setPAYER_ACCT(String PAYER_ACCT) {
        this.PAYER_ACCT = PAYER_ACCT;
    }

    @Override
    public String toString() {
        return "TRANSACTION{" +
                "PF_NO='" + PF_NO + '\'' +
                ", NAME='" + NAME + '\'' +
                ", ACCOUNT='" + ACCOUNT + '\'' +
                ", REFERENCE='" + REFERENCE + '\'' +
                ", CREDIT_TO_DQA='" + CREDIT_TO_DQA + '\'' +
                ", BRANCHNO='" + BRANCHNO + '\'' +
                ", PAYER_ACCT='" + PAYER_ACCT + '\'' +
                '}';
    }
}
