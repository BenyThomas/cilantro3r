package com.DTO.IBANK;

import lombok.Data;

import jakarta.xml.bind.annotation.*;
import java.math.BigDecimal;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "responseCode",
        "reference",
        "receipt",
        "message",
        "availableBalance",
        "ledgerBalance"
})
@XmlRootElement(name = "eMkopoResponse")

public class EmkopoResp {
    @XmlElement(name = "responseCode", required = true)
    public String responseCode;
    @XmlElement(name = "message", required = true)
    public String message;
    @XmlElement(name = "reference", required = true)
    public String reference;
    @XmlElement(name = "availableBalance", required = true)
    public BigDecimal availableBalance;
    @XmlElement(name = "ledgerBalance", required = true)
    public BigDecimal ledgerBalance;
    @XmlElement(name = "receipt", required = true)
    public String receipt;
    @XmlElement(name = "swiftMessage", required = true)
    public String swiftMessage;

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public void setSwiftMessage(String swiftMessage){this.swiftMessage = swiftMessage;}

    public String getSwiftMessage(){return swiftMessage;}

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(BigDecimal availableBalance) {
        this.availableBalance = availableBalance;
    }

    public BigDecimal getLedgerBalance() {
        return ledgerBalance;
    }

    public void setLedgerBalance(BigDecimal ledgerBalance) {
        this.ledgerBalance = ledgerBalance;
    }

    public String getReceipt() {
        return receipt;
    }

    public void setReceipt(String receipt) {
        this.receipt = receipt;
    }

    @Override
    public String toString() {
        return "PaymentResp{" + "responseCode=" + responseCode + ", message=" + message + ", reference=" + reference + ", availableBalance=" + availableBalance + ", ledgerBalance=" + ledgerBalance + ", swiftMessage=" + swiftMessage + ", receipt=" + receipt + '}';
    }
}
