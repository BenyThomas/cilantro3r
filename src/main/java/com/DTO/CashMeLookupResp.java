package com.DTO;

import java.math.BigDecimal;

public class CashMeLookupResp {
    private String trxnRef;
    private String responseCode;
    private String responseMessage;
    private String bankBic;
    private String acctNo;
    private String acctName;
    private String currency;
    private BigDecimal minDeposit;
    private String notifyFlag;

    public String getTrxnRef() {
        return trxnRef;
    }

    public void setTrxnRef(String trxnRef) {
        this.trxnRef = trxnRef;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    public String getBankBic() {
        return bankBic;
    }

    public void setBankBic(String bankBic) {
        this.bankBic = bankBic;
    }

    public String getAcctNo() {
        return acctNo;
    }

    public void setAcctNo(String acctNo) {
        this.acctNo = acctNo;
    }

    public String getAcctName() {
        return acctName;
    }

    public void setAcctName(String acctName) {
        this.acctName = acctName;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getMinDeposit() {
        return minDeposit;
    }

    public void setMinDeposit(BigDecimal minDeposit) {
        this.minDeposit = minDeposit;
    }

    public String getNotifyFlag() {
        return notifyFlag;
    }

    public void setNotifyFlag(String notifyFlag) {
        this.notifyFlag = notifyFlag;
    }

    @Override
    public String toString() {
        return "CashMeLookupResp{" +
                "trxnRef='" + trxnRef + '\'' +
                ", responseCode='" + responseCode + '\'' +
                ", responseMessage='" + responseMessage + '\'' +
                ", bankBic='" + bankBic + '\'' +
                ", acctNo='" + acctNo + '\'' +
                ", acctName='" + acctName + '\'' +
                ", currency='" + currency + '\'' +
                ", minDeposit=" + minDeposit +
                ", notifyFlag='" + notifyFlag + '\'' +
                '}';
    }
}
