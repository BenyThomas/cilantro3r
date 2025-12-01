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
@XmlType(name = "Gepg", propOrder = {
    "gepgBillQryResp",
    "gepgSignature"
})
public class GepgBillQry
{
    private GepgBillQryResp gepgBillQryResp;

    private String gepgSignature;

    public GepgBillQryResp getGepgBillQryResp ()
    {
        return gepgBillQryResp;
    }

    public void setGepgBillQryResp (GepgBillQryResp gepgBillQryResp)
    {
        this.gepgBillQryResp = gepgBillQryResp;
    }

    public String getGepgSignature ()
    {
        return gepgSignature;
    }

    public void setGepgSignature (String gepgSignature)
    {
        this.gepgSignature = gepgSignature;
    }

    @Override
    public String toString()
    {
        return "[gepgBillQryResp = "+gepgBillQryResp+", gepgSignature = "+gepgSignature+"]";
    }
}
			
		