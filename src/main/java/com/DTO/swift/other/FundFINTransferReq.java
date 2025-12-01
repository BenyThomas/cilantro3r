/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.swift.other;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 *
 * @author melleji.mollel
 */
@Getter
@Setter
@ToString
public class FundFINTransferReq {

    public String senderBic;
    public String receiverBic;
    public String msgType; //TISS OR EFT use TISS
    public String bankRef;
    public String bankOperationCode; //CRED
    public String tranDate;
    public String currency;
    public String tranAmount;
    public String senderAccount;
    public String senderName;
    public String senderPhoneNo;
    public String senderEmailAddress;
    public String senderCorrespondent;
    public String receiverAccount;
    public String receiverName;
    public String tranDesc;
    public String detailOfCharge; //OUR
    public String isLocalOrInternational; //OUR
    public String contraAccount; //OUR
    public String chargeScheme; //OUR
}
