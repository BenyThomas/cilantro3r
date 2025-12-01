/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;

/**
 *
 * @author Melleji Mollel
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class BillTrxDtl
{
    private String BillCtrNum;

    private String BillPayOpt;

    private String BillDesc;

    private String BillExprDt;

    private String CCy;

    private String PyrName;
    private String PyrCellNum;
    private String PyrEmailAddr;

    private String BillAmt;

    private BillItems BillItems;

    public String getBillCtrNum ()
    {
        return BillCtrNum;
    }

    public void setBillCtrNum (String BillCtrNum)
    {
        this.BillCtrNum = BillCtrNum;
    }

    public String getBillPayOpt ()
    {
        return BillPayOpt;
    }

    public void setBillPayOpt (String BillPayOpt)
    {
        this.BillPayOpt = BillPayOpt;
    }

    public String getBillDesc ()
    {
        return BillDesc;
    }

    public void setBillDesc (String BillDesc)
    {
        this.BillDesc = BillDesc;
    }

    public String getBillExprDt ()
    {
        return BillExprDt;
    }

    public void setBillExprDt (String BillExprDt)
    {
        this.BillExprDt = BillExprDt;
    }

    public String getCCy ()
    {
        return CCy;
    }

    public void setCCy (String CCy)
    {
        this.CCy = CCy;
    }

    public String getPyrName ()
    {
        return PyrName;
    }

    public void setPyrName (String PyrName)
    {
        this.PyrName = PyrName;
    }

    public String getBillAmt ()
    {
        return BillAmt;
    }

    public void setBillAmt (String BillAmt)
    {
        this.BillAmt = BillAmt;
    }

    public BillItems getBillItems ()
    {
        return BillItems;
    }

    public void setBillItems (BillItems BillItems)
    {
        this.BillItems = BillItems;
    }

    public String getPyrCellNum() {
        return PyrCellNum;
    }

    public void setPyrCellNum(String PyrCellNum) {
        this.PyrCellNum = PyrCellNum;
    }

    public String getPyrEmailAddr() {
        return PyrEmailAddr;
    }

    public void setPyrEmailAddr(String PyrEmailAddr) {
        this.PyrEmailAddr = PyrEmailAddr;
    }

    @Override
    public String toString()
    {
        return "BillTrxDtl [BillCtrNum = "+BillCtrNum+", BillPayOpt = "+BillPayOpt+", BillDesc = "+BillDesc+", BillExprDt = "+BillExprDt+", CCy = "+CCy+", PyrName = "+PyrName+", BillAmt = "+BillAmt+", BillItems = "+BillItems+"]";
    }
}
			
		