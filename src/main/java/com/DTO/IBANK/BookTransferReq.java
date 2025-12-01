/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.IBANK;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author melleji.mollel Feb 13, 2021 7:41:21 PM
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "reference",
    "sourceAccount",
    "beneficiaryAccount",
    "currency",
    "amount",
    "exchangeRate",
    "description",
})
@XmlRootElement(name = "bookTransferRequest")
public class BookTransferReq {

}
