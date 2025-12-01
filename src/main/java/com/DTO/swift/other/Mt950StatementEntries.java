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
public class Mt950StatementEntries {

    public String relatedReference;
    public String transDate;
    public String currency;
    public String messageType;
    public String drCrIndicator;
    public String senderBank;
    public String beneficiaryBank;
    public BigDecimal amount;
    public BigDecimal prevoiusBalance;
    public BigDecimal postBalance;
}
