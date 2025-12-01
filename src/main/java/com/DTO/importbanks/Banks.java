/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.importbanks;

import java.util.List;

/**
 *
 * @author melleji.mollel
 */
public class Banks {
    public String country;
    public String country_code;
    public List<BanksList> list;

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCountry_code() {
        return country_code;
    }

    public void setCountry_code(String country_code) {
        this.country_code = country_code;
    }

    public List<BanksList> getList() {
        return list;
    }

    public void setList(List<BanksList> list) {
        this.list = list;
    }

    @Override
    public String toString() {
        return "Banks{" + "country=" + country + ", country_code=" + country_code + ", list=" + list + '}';
    }

   
    
}
