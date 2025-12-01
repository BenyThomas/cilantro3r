package com.DTO;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
public class visaCardTracingObject {
    private  String serviceType;
    private String accountNo;
    private String phone;
    private String customerName;
    private String customerRimNo;
    private String reference;
    private String errorEncountered;
    private String actionToTake;
    private String sqlCheck;
    private String actionStatus;
    private String checksum;
}
