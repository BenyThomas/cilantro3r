/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.recon;

import java.math.BigDecimal;

/**
 *
 * @author melleji.mollel
 */
public class AdjustedRecon {

    public String txnCountCreditsNotInCBS;
    public BigDecimal txnVolumeCreditsNotInCBS;
    public String txnCountCreditsNotInThirdParty;
    public BigDecimal txnVolumeCreditsNotInThirdPart;
    public String txnCountDebitsNotInCBS;
    public BigDecimal txnVolumeDebitsNotInCBS;
    public String txnCountDebitsNotInThirdParty;
    public BigDecimal txnVolumeDebitsNotInThirdParty;
    public BigDecimal cbsClosingBalance;
    public BigDecimal thirdpartyClosingBalance;
    public String cashBookOpeningBalance;

    public String getTxnCountCreditsNotInCBS() {
        return txnCountCreditsNotInCBS;
    }

    public void setTxnCountCreditsNotInCBS(String txnCountCreditsNotInCBS) {
        this.txnCountCreditsNotInCBS = txnCountCreditsNotInCBS;
    }

    public BigDecimal getTxnVolumeCreditsNotInCBS() {
        return txnVolumeCreditsNotInCBS;
    }

    public void setTxnVolumeCreditsNotInCBS(BigDecimal txnVolumeCreditsNotInCBS) {
        this.txnVolumeCreditsNotInCBS = txnVolumeCreditsNotInCBS;
    }

    public String getTxnCountCreditsNotInThirdParty() {
        return txnCountCreditsNotInThirdParty;
    }

    public void setTxnCountCreditsNotInThirdParty(String txnCountCreditsNotInThirdParty) {
        this.txnCountCreditsNotInThirdParty = txnCountCreditsNotInThirdParty;
    }

    public BigDecimal getTxnVolumeCreditsNotInThirdPart() {
        return txnVolumeCreditsNotInThirdPart;
    }

    public void setTxnVolumeCreditsNotInThirdPart(BigDecimal txnVolumeCreditsNotInThirdPart) {
        this.txnVolumeCreditsNotInThirdPart = txnVolumeCreditsNotInThirdPart;
    }

    public String getTxnCountDebitsNotInCBS() {
        return txnCountDebitsNotInCBS;
    }

    public void setTxnCountDebitsNotInCBS(String txnCountDebitsNotInCBS) {
        this.txnCountDebitsNotInCBS = txnCountDebitsNotInCBS;
    }

    public BigDecimal getTxnVolumeDebitsNotInCBS() {
        return txnVolumeDebitsNotInCBS;
    }

    public void setTxnVolumeDebitsNotInCBS(BigDecimal txnVolumeDebitsNotInCBS) {
        this.txnVolumeDebitsNotInCBS = txnVolumeDebitsNotInCBS;
    }

    public String getTxnCountDebitsNotInThirdParty() {
        return txnCountDebitsNotInThirdParty;
    }

    public void setTxnCountDebitsNotInThirdParty(String txnCountDebitsNotInThirdParty) {
        this.txnCountDebitsNotInThirdParty = txnCountDebitsNotInThirdParty;
    }

    public BigDecimal getTxnVolumeDebitsNotInThirdParty() {
        return txnVolumeDebitsNotInThirdParty;
    }

    public void setTxnVolumeDebitsNotInThirdParty(BigDecimal txnVolumeDebitsNotInThirdParty) {
        this.txnVolumeDebitsNotInThirdParty = txnVolumeDebitsNotInThirdParty;
    }

    public BigDecimal getCbsClosingBalance() {
        return cbsClosingBalance;
    }

    public void setCbsClosingBalance(BigDecimal cbsClosingBalance) {
        this.cbsClosingBalance = cbsClosingBalance;
    }

    public BigDecimal getThirdpartyClosingBalance() {
        return thirdpartyClosingBalance;
    }

    public void setThirdpartyClosingBalance(BigDecimal thirdpartyClosingBalance) {
        this.thirdpartyClosingBalance = thirdpartyClosingBalance;
    }

    public String getCashBookOpeningBalance() {
        return cashBookOpeningBalance;
    }

    public void setCashBookOpeningBalance(String cashBookOpeningBalance) {
        this.cashBookOpeningBalance = cashBookOpeningBalance;
    }

    @Override
    public String toString() {
        return "AdjustedRecon{" + "txnCountCreditsNotInCBS=" + txnCountCreditsNotInCBS + ", txnVolumeCreditsNotInCBS=" + txnVolumeCreditsNotInCBS + ", txnCountCreditsNotInThirdParty=" + txnCountCreditsNotInThirdParty + ", txnVolumeCreditsNotInThirdPart=" + txnVolumeCreditsNotInThirdPart + ", txnCountDebitsNotInCBS=" + txnCountDebitsNotInCBS + ", txnVolumeDebitsNotInCBS=" + txnVolumeDebitsNotInCBS + ", txnCountDebitsNotInThirdParty=" + txnCountDebitsNotInThirdParty + ", txnVolumeDebitsNotInThirdParty=" + txnVolumeDebitsNotInThirdParty + ", cbsClosingBalance=" + cbsClosingBalance + ", thirdpartyClosingBalance=" + thirdpartyClosingBalance + ", cashBookOpeningBalance=" + cashBookOpeningBalance + '}';
    }


}
