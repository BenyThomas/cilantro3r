/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO;

import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;

/**
 *
 * @author Melleji Mollel
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class BillItems
{
    private List<BillItem> BillItem;

    public List<BillItem> getBillItem ()
    {
        return BillItem;
    }

    public void setBillItem (List<BillItem> BillItem)
    {
        this.BillItem = BillItem;
    }

    @Override
    public String toString()
    {
        return "BillItems [BillItem = "+BillItem+"]";
    }
}
