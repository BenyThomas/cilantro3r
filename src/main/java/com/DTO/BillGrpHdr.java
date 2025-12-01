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
 * @author melleji Mollel
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class BillGrpHdr
{
    private String pspBillReqId;

    private String gepgBillReqId;

    private String SpCode;

    private String BillStsCode;

    private String SpName;

    public String getPspBillReqId ()
    {
        return pspBillReqId;
    }

    public void setPspBillReqId (String pspBillReqId)
    {
        this.pspBillReqId = pspBillReqId;
    }

    public String getGepgBillReqId ()
    {
        return gepgBillReqId;
    }

    public void setGepgBillReqId (String gepgBillReqId)
    {
        this.gepgBillReqId = gepgBillReqId;
    }

    public String getSpCode ()
    {
        return SpCode;
    }

    public void setSpCode (String SpCode)
    {
        this.SpCode = SpCode;
    }

    public String getBillStsCode ()
    {
        return BillStsCode;
    }

    public void setBillStsCode (String BillStsCode)
    {
        this.BillStsCode = BillStsCode;
    }

    public String getSpName ()
    {
        return SpName;
    }

    public void setSpName (String SpName)
    {
        this.SpName = SpName;
    }

    @Override
    public String toString()
    {
        return "BillGrpHdr [pspBillReqId = "+pspBillReqId+", gepgBillReqId = "+gepgBillReqId+", SpCode = "+SpCode+", BillStsCode = "+BillStsCode+", SpName = "+SpName+"]";
    }
}
		
