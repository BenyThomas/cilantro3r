/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author Melleji Mollel
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "gepgBillQryResp", propOrder = {
    "BillGrpHdr",
    "BillTrxDtl"
})
public class GepgBillQryResp
{
    private BillGrpHdr BillGrpHdr;

    private BillTrxDtl BillTrxDtl;

    public BillGrpHdr getBillGrpHdr ()
    {
        return BillGrpHdr;
    }

    public void setBillGrpHdr (BillGrpHdr BillGrpHdr)
    {
        this.BillGrpHdr = BillGrpHdr;
    }

    public BillTrxDtl getBillTrxDtl ()
    {
        return BillTrxDtl;
    }

    public void setBillTrxDtl (BillTrxDtl BillTrxDtl)
    {
        this.BillTrxDtl = BillTrxDtl;
    }

    @Override
    public String toString()
    {
        return "GepgBillQryResp [BillGrpHdr = "+BillGrpHdr+", BillTrxDtl = "+BillTrxDtl+"]";
    }
}