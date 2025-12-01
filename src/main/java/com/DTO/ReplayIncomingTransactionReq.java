/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import philae.api.UsRole;

/**
 *
 * @author melleji.mollel Mar 9, 2021 8:27:09 PM
 */
@Getter
@Setter
@ToString
public class ReplayIncomingTransactionReq {

    public String reference;
    public String destinationAcct;
    public UsRole usRole;
    public String identifier;
    public String toDestinationAccts;
    public String desicionCode;
}
