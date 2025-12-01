/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.swift.other;

import java.math.BigDecimal;
import java.util.List;
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
public class Mt950ObjectReq {

    public String account;
    public String reference;
    public String statementNo;
    public String sequenceNo;
    public String isOpeningDebitOrCredit;
    public BigDecimal openingBalance;
    public BigDecimal closingBalance;
    public String transDate;
    public String currency;
    public List<Mt950StatementEntries> statementEntries;

}
