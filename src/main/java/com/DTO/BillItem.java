/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/**
 *
 * @author Melleji Mollel
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class BillItem {

    private String BillItemAmt;

    private String BillItemRef;

    private String BillItemMiscAmt;

    private String BillItemDesc;

    private String GfsCode;

    public String getBillItemAmt() {
        return BillItemAmt;
    }

    public void setBillItemAmt(String BillItemAmt) {
        this.BillItemAmt = BillItemAmt;
    }

    public String getBillItemRef() {
        return BillItemRef;
    }

    public void setBillItemRef(String BillItemRef) {
        this.BillItemRef = BillItemRef;
    }

    public String getBillItemMiscAmt() {
        return BillItemMiscAmt;
    }

    public void setBillItemMiscAmt(String BillItemMiscAmt) {
        this.BillItemMiscAmt = BillItemMiscAmt;
    }

    public String getBillItemDesc() {
        return BillItemDesc;
    }

    public void setBillItemDesc(String BillItemDesc) {
        this.BillItemDesc = BillItemDesc;
    }

    public String getGfsCode() {
        return GfsCode;
    }

    public void setGfsCode(String GfsCode) {
        this.GfsCode = GfsCode;
    }
    @Override
    public String toString() {
        return "BillItem [BillItemAmt = " + BillItemAmt + ", BillItemRef = " + BillItemRef + ", BillItemMiscAmt = " + BillItemMiscAmt + ", BillItemDesc = " + BillItemDesc + ", GfsCode = " + GfsCode + "]";
    }
}
