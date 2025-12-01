/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO;

import philae.api.UsRole;

/**
 *
 * @author melleji.mollel Mar 9, 2021 8:27:09 PM
 */
public class EftReplayTransactionReq {

    public String reference;
    public String destinationAcct;
    public UsRole usRole;
    public String identifier;

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getDestinationAcct() {
        return destinationAcct;
    }

    public void setDestinationAcct(String destinationAcct) {
        this.destinationAcct = destinationAcct;
    }

    public UsRole getUsRole() {
        return usRole;
    }

    public void setUsRole(UsRole usRole) {
        this.usRole = usRole;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public String toString() {
        return "EftReplayTransactionReq{" + "reference=" + reference + ", destinationAcct=" + destinationAcct + ", usRole=" + usRole + ", identifier=" + identifier + '}';
    }

   

}
