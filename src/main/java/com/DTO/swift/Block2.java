/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.swift;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 *
 * @author melleji.mollel
 */
@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PUBLIC)
@ToString

public class Block2 {

    public String receiverAddress;
    public String messagePriority;
    public String messageType;
    public String direction;
    public String senderInputTime;
    public String MIRDate;
    public String MIRLogicalTerminal;
    public String MIRSessionNumber;
    public String receiverOutputDate;
    public String receiverOutputTime;

}
