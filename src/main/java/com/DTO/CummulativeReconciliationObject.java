/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO;

import java.math.BigDecimal;

/**
 *
 * @author melleji.mollel
 */
public class CummulativeReconciliationObject {
     public BigDecimal cashBookOpeningBalance;
    public BigDecimal cashBookRefundFosa;
    public BigDecimal cashBookCustomerWithraws;
    public BigDecimal cashBookCustomerWithrawsCharges;
    public BigDecimal cashBookLedgerFee;
    public BigDecimal cashBookClosingBalance;
    public BigDecimal bankClosingBalance;
    public BigDecimal bankUncreditedCheques;
    public BigDecimal bankUncreditedDeposits;
    public BigDecimal bankCashWithdrawsNotInFosa;
    public BigDecimal bankTransactionWithdrawsCharges;
    public BigDecimal bankUncommissionedCustomerWithdraws;
    public BigDecimal bankUncommissionedCustomerWithdrawsCharges;
    public BigDecimal bankDepositsNotInCashbook;
    public BigDecimal bankTransactionCharges;
    public BigDecimal bankLedgerFees;
    public BigDecimal cashBookRefundFosaLessBack;
    public BigDecimal bankOpeningBalance;
    public BigDecimal bankPreviousTransactionCharges;

    public BigDecimal getCashBookOpeningBalance() {
        return cashBookOpeningBalance;
    }

    public void setCashBookOpeningBalance(BigDecimal cashBookOpeningBalance) {
        this.cashBookOpeningBalance = cashBookOpeningBalance;
    }

    public BigDecimal getCashBookRefundFosa() {
        return cashBookRefundFosa;
    }

    public void setCashBookRefundFosa(BigDecimal cashBookRefundFosa) {
        this.cashBookRefundFosa = cashBookRefundFosa;
    }

    public BigDecimal getCashBookCustomerWithraws() {
        return cashBookCustomerWithraws;
    }

    public void setCashBookCustomerWithraws(BigDecimal cashBookCustomerWithraws) {
        this.cashBookCustomerWithraws = cashBookCustomerWithraws;
    }

    public BigDecimal getCashBookCustomerWithrawsCharges() {
        return cashBookCustomerWithrawsCharges;
    }

    public void setCashBookCustomerWithrawsCharges(BigDecimal cashBookCustomerWithrawsCharges) {
        this.cashBookCustomerWithrawsCharges = cashBookCustomerWithrawsCharges;
    }

    public BigDecimal getCashBookLedgerFee() {
        return cashBookLedgerFee;
    }

    public void setCashBookLedgerFee(BigDecimal cashBookLedgerFee) {
        this.cashBookLedgerFee = cashBookLedgerFee;
    }

    public BigDecimal getCashBookClosingBalance() {
        return cashBookClosingBalance;
    }

    public void setCashBookClosingBalance(BigDecimal cashBookClosingBalance) {
        this.cashBookClosingBalance = cashBookClosingBalance;
    }

    public BigDecimal getBankClosingBalance() {
        return bankClosingBalance;
    }

    public void setBankClosingBalance(BigDecimal bankClosingBalance) {
        this.bankClosingBalance = bankClosingBalance;
    }

    public BigDecimal getBankUncreditedCheques() {
        return bankUncreditedCheques;
    }

    public void setBankUncreditedCheques(BigDecimal bankUncreditedCheques) {
        this.bankUncreditedCheques = bankUncreditedCheques;
    }

    public BigDecimal getBankUncreditedDeposits() {
        return bankUncreditedDeposits;
    }

    public void setBankUncreditedDeposits(BigDecimal bankUncreditedDeposits) {
        this.bankUncreditedDeposits = bankUncreditedDeposits;
    }

    public BigDecimal getBankCashWithdrawsNotInFosa() {
        return bankCashWithdrawsNotInFosa;
    }

    public void setBankCashWithdrawsNotInFosa(BigDecimal bankCashWithdrawsNotInFosa) {
        this.bankCashWithdrawsNotInFosa = bankCashWithdrawsNotInFosa;
    }

    public BigDecimal getBankTransactionWithdrawsCharges() {
        return bankTransactionWithdrawsCharges;
    }

    public void setBankTransactionWithdrawsCharges(BigDecimal bankTransactionWithdrawsCharges) {
        this.bankTransactionWithdrawsCharges = bankTransactionWithdrawsCharges;
    }

    public BigDecimal getBankUncommissionedCustomerWithdraws() {
        return bankUncommissionedCustomerWithdraws;
    }

    public void setBankUncommissionedCustomerWithdraws(BigDecimal bankUncommissionedCustomerWithdraws) {
        this.bankUncommissionedCustomerWithdraws = bankUncommissionedCustomerWithdraws;
    }

    public BigDecimal getBankUncommissionedCustomerWithdrawsCharges() {
        return bankUncommissionedCustomerWithdrawsCharges;
    }

    public void setBankUncommissionedCustomerWithdrawsCharges(BigDecimal bankUncommissionedCustomerWithdrawsCharges) {
        this.bankUncommissionedCustomerWithdrawsCharges = bankUncommissionedCustomerWithdrawsCharges;
    }

    public BigDecimal getBankDepositsNotInCashbook() {
        return bankDepositsNotInCashbook;
    }

    public void setBankDepositsNotInCashbook(BigDecimal bankDepositsNotInCashbook) {
        this.bankDepositsNotInCashbook = bankDepositsNotInCashbook;
    }

    public BigDecimal getBankTransactionCharges() {
        return bankTransactionCharges;
    }

    public void setBankTransactionCharges(BigDecimal bankTransactionCharges) {
        this.bankTransactionCharges = bankTransactionCharges;
    }

    public BigDecimal getBankLedgerFees() {
        return bankLedgerFees;
    }

    public void setBankLedgerFees(BigDecimal bankLedgerFees) {
        this.bankLedgerFees = bankLedgerFees;
    }

    public BigDecimal getCashBookRefundFosaLessBack() {
        return cashBookRefundFosaLessBack;
    }

    public void setCashBookRefundFosaLessBack(BigDecimal cashBookRefundFosaLessBack) {
        this.cashBookRefundFosaLessBack = cashBookRefundFosaLessBack;
    }

    public BigDecimal getBankOpeningBalance() {
        return bankOpeningBalance;
    }

    public void setBankOpeningBalance(BigDecimal bankOpeningBalance) {
        this.bankOpeningBalance = bankOpeningBalance;
    }

    public BigDecimal getBankPreviousTransactionCharges() {
        return bankPreviousTransactionCharges;
    }

    public void setBankPreviousTransactionCharges(BigDecimal bankPreviousTransactionCharges) {
        this.bankPreviousTransactionCharges = bankPreviousTransactionCharges;
    }

    @Override
    public String toString() {
        return "CummulativeReconciliationObject{" + "cashBookOpeningBalance=" + cashBookOpeningBalance + ", cashBookRefundFosa=" + cashBookRefundFosa + ", cashBookCustomerWithraws=" + cashBookCustomerWithraws + ", cashBookCustomerWithrawsCharges=" + cashBookCustomerWithrawsCharges + ", cashBookLedgerFee=" + cashBookLedgerFee + ", cashBookClosingBalance=" + cashBookClosingBalance + ", bankClosingBalance=" + bankClosingBalance + ", bankUncreditedCheques=" + bankUncreditedCheques + ", bankUncreditedDeposits=" + bankUncreditedDeposits + ", bankCashWithdrawsNotInFosa=" + bankCashWithdrawsNotInFosa + ", bankTransactionWithdrawsCharges=" + bankTransactionWithdrawsCharges + ", bankUncommissionedCustomerWithdraws=" + bankUncommissionedCustomerWithdraws + ", bankUncommissionedCustomerWithdrawsCharges=" + bankUncommissionedCustomerWithdrawsCharges + ", bankDepositsNotInCashbook=" + bankDepositsNotInCashbook + ", bankTransactionCharges=" + bankTransactionCharges + ", bankLedgerFees=" + bankLedgerFees + ", cashBookRefundFosaLessBack=" + cashBookRefundFosaLessBack + ", bankOpeningBalance=" + bankOpeningBalance + ", bankPreviousTransactionCharges=" + bankPreviousTransactionCharges + '}';
    }

   
}
