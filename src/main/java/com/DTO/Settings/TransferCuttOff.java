/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.Settings;

/**
 *
 * @author melleji.mollel
 */
public class TransferCuttOff {
    public String transferType;
    public String dayOfWeek;
    public String toTime;

    public String getTransferType() {
        return transferType;
    }

    public void setTransferType(String transferType) {
        this.transferType = transferType;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getToTime() {
        return toTime;
    }

    public void setToTime(String toTime) {
        this.toTime = toTime;
    }

    @Override
    public String toString() {
        return "TransferCuttOff{" + "transferType=" + transferType + ", dayOfWeek=" + dayOfWeek + ", toTime=" + toTime + '}';
    }
    
}
