/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.swift.other;

import java.math.BigDecimal;
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
public class TransferAdviceReq {

    public String messageType;
    public String senderBank;
    public String receiverBank;
    public String senderAccount;
    public String receiverAccount;
    public String direction;
    public String senderReference;
    public String relatedReference;
    public String transDate;
    public String currency;
    public BigDecimal amount;
    public String serviceCode;
    public String cbsStatus;
    public String cbsMessage;
    public String status;
     public String swiftMessage;
     public String channel;
    public byte[] messageInPdf;

}
