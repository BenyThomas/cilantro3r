/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO;

import java.io.Serializable;
import philae.api.BnUser;
import philae.api.UsRole;

/**
 *
 * @author melleji.mollel
 */
public class RemittanceToQueue implements Serializable {

    public String references;
    public BnUser bnUser;
    public UsRole usRole;
    public philae.ach.UsRole usAchRole;

    public String getReferences() {
        return references;
    }

    public void setReferences(String references) {
        this.references = references;
    }

    public BnUser getBnUser() {
        return bnUser;
    }

    public void setBnUser(BnUser bnUser) {
        this.bnUser = bnUser;
    }

    public UsRole getUsRole() {
        return usRole;
    }

    public void setUsRole(UsRole usRole) {
        this.usRole = usRole;
    }

    public philae.ach.UsRole getUsAchRole() {
        return usAchRole;
    }

    public void setUsAchRole(philae.ach.UsRole usAchRole) {
        this.usAchRole = usAchRole;
    }

    @Override
    public String toString() {
        return "RemittanceToQueue{" + "references=" + references + ", bnUser=" + bnUser + ", usRole=" + usRole + ", usAchRole=" + usAchRole + '}';
    }

    
}
