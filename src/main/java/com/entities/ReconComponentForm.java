/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.entities;

import javax.validation.constraints.NotBlank;

/**
 *
 * @author melleji.mollel
 */
public class ReconComponentForm {

    @NotBlank(message = "Component Name Is required")
    private String name;
    @NotBlank(message = "Component Code is Required")
    private String code;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return "ReconComponentForm{" + "name=" + name + ", code=" + code + '}';
    }

}
