/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.entities;

import jakarta.validation.constraints.NotBlank;

/**
 *
 * @author MELLEJI
 */
public class ReportsForm2 {

    @NotBlank(message = "Please select Transaction type")
    private String txnType;
    
    @NotBlank(message = "Please select Transaction period")
    private String period;

    @NotBlank(message = "Please select Transaction month")
    private String month;

    private String year;

    public String getTxnType() {
        return txnType;
    }

    public void setTxnType(String txnType) {
        this.txnType = txnType;
    }

 
    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

}
