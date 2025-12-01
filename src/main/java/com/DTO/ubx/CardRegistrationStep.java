package com.DTO.ubx;

public enum CardRegistrationStep {
    L("LINK CARD"),
    A("ACTIVATE CARD"),
    PR("PIN RESET"),
    R("REGISTER CARD TO CBS"),
    C("CHARGE CARD"),
    PC("PIN CHANGE"),
    N("NOTIFICATION");

    private final String name;
    private CardRegistrationStep(String name) {this.name = name;}
    public String getName() {return name;}
    @Override
    public String toString() {return name;}
}
