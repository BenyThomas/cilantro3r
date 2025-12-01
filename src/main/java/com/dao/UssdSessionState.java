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
public class UssdSessionState {

    private String sessionId;
    private int currentChoice;
    private int state;
    private int previousState;
    private int inputType;
    private String reqString;
    private String menuVariables;
    private int language;
    private int next_custom_input;
    private int new_state_error;

    public int getNew_state_error() {
        return new_state_error;
    }

    public void setNew_state_error(int new_state_error) {
        this.new_state_error = new_state_error;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public int getCurrentChoice() {
        return currentChoice;
    }

    public void setCurrentChoice(int currentChoice) {
        this.currentChoice = currentChoice;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getPreviousState() {
        return previousState;
    }

    public void setPreviousState(int previousState) {
        this.previousState = previousState;
    }

    public int getInputType() {
        return inputType;
    }

    public void setInputType(int inputType) {
        this.inputType = inputType;
    }

    public String getReqString() {
        return reqString;
    }

    public void setReqString(String reqString) {
        this.reqString = reqString;
    }

    public String getMenuVariables() {
        return menuVariables;
    }

    public void setMenuVariables(String menuVariables) {
        this.menuVariables = menuVariables;
    }

    public int getLanguage() {
        return language;
    }

    public void setLanguage(int language) {
        this.language = language;
    }

    public int getNext_custom_input() {
        return next_custom_input;
    }

    public void setNext_custom_input(int next_custom_input) {
        this.next_custom_input = next_custom_input;
    }

    @Override
    public String toString() {
        return "UssdSessionState{" + "sessionId=" + sessionId + ", currentChoice=" + currentChoice + ", state=" + state + ", previousState=" + previousState + ", inputType=" + inputType + ", reqString=" + reqString + ", menuVariables=" + menuVariables + ", language=" + language + ", next_custom_input=" + next_custom_input + ", new_state_error=" + new_state_error + '}';
    }

}
