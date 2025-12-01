/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.Ebanking;

/**
 *
 * @author melleji.mollel
 */
public class BCXCardCreationResp {
       private String data;

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "BCXCardCreationResp{" + "data=" + data + '}';
    }
}
