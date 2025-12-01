/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.importbanks;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author melleji.mollel
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BanksList {

    @JsonProperty("id")
    public String id;
    @JsonProperty("bank")
    public String bank;
    @JsonProperty("city")
    public String city;
    @JsonProperty("branch")
    public String branch;
    @JsonProperty("swift_code")
    public String swift_code;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBank() {
        return bank;
    }

    public void setBank(String bank) {
        this.bank = bank;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getSwift_code() {
        return swift_code;
    }

    public void setSwift_code(String swift_code) {
        this.swift_code = swift_code;
    }

    @Override
    public String toString() {
        return "BanksList{" + "id=" + id + ", bank=" + bank + ", city=" + city + ", branch=" + branch + ", swift_code=" + swift_code + '}';
    }

}
