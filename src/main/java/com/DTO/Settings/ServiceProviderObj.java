package com.DTO.Settings;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class ServiceProviderObj {
    private int id;
    private String name;
    private String address;
    private String phone;
    private String bankSwiftCode;
    private String intermediaryBank;
    private String bankAccount;
    private String facilityDescription;
    private String identifier;
}
