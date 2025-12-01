/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dao;

/**
 *
 * @author MELLEJI
 */
public class UssdResponse {

    private String title;
    private String bodyMsg;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBodyMsg() {
        return bodyMsg;
    }

    public void setBodyMsg(String bodyMsg) {
        this.bodyMsg = bodyMsg;
    }

    @Override
    public String toString() {
        return "UssdResponse{" + "title=" + title + ", bodyMsg=" + bodyMsg + '}';
    }

}
