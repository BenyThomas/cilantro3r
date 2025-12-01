/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.ReqToRubikon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "userRole", propOrder = {
    "limits",
    "drawers",
    "branchCode",
    "branchId",
    "branchName",
    "buRoleId",
    "role",
    "roleId",
    "supervisor",
    "userRole",
    "userId",
    "userName",
    "userRoleId"})
@Getter
@Setter
public class UserRole {

    @XmlElement(name = "limits")
    public Limits limits;
    @XmlElement(name = "drawers")
    public Drawers drawers;
    @XmlElement(name = "branchCode")
    public String branchCode;
    @XmlElement(name = "branchId")
    public String branchId;
    @XmlElement(name = "branchName")
    public String branchName;
    @XmlElement(name = "buRoleId")
    public String buRoleId;
    @XmlElement(name = "role")
    public String role;
    @XmlElement(name = "roleId")
    public String roleId;
    @XmlElement(name = "supervisor")
    public String supervisor;
    @XmlElement(name = "userRole")
    public String userRole;
    @XmlElement(name = "userId")
    public String userId;
    @XmlElement(name = "userName")
    public String userName;
    @XmlElement(name = "userRoleId")
    public String userRoleId;

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
}
