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
public class UssdUserChoice {

    private String ussdCode;
    private String businessCode;
    private String state;
    private String choice;
    private String name;
    private String inputType;
    private String DoCode;
    private String newState;
    private String customInput;
    private String customVariable;
    private String new_state_error;

    public String getNew_state_error() {
        return new_state_error;
    }

    public void setNew_state_error(String new_state_error) {
        this.new_state_error = new_state_error;
    }

    public String getUssdCode() {
        return ussdCode;
    }

    public void setUssdCode(String ussdCode) {
        this.ussdCode = ussdCode;
    }

    public String getBusinessCode() {
        return businessCode;
    }

    public void setBusinessCode(String businessCode) {
        this.businessCode = businessCode;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getChoice() {
        return choice;
    }

    public void setChoice(String choice) {
        this.choice = choice;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInputType() {
        return inputType;
    }

    public void setInputType(String inputType) {
        this.inputType = inputType;
    }

    public String getDoCode() {
        return DoCode;
    }

    public void setDoCode(String DoCode) {
        this.DoCode = DoCode;
    }

    public String getNewState() {
        return newState;
    }

    public void setNewState(String newState) {
        this.newState = newState;
    }

    public String getCustomInput() {
        return customInput;
    }

    public void setCustomInput(String customInput) {
        this.customInput = customInput;
    }

    public String getCustomVariable() {
        return customVariable;
    }

    public void setCustomVariable(String customVariable) {
        this.customVariable = customVariable;
    }

    @Override
    public String toString() {
        return "UssdUserChoice{" + "ussdCode=" + ussdCode + ", businessCode=" + businessCode + ", state=" + state + ", choice=" + choice + ", name=" + name + ", inputType=" + inputType + ", DoCode=" + DoCode + ", newState=" + newState + ", customInput=" + customInput + ", customVariable=" + customVariable + ", new_state_error=" + new_state_error + '}';
    }

}
