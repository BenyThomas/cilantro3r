package com.DTO.salary;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import jakarta.xml.bind.annotation.XmlElement;


@Getter
@Setter
@ToString
public class LoanRepaymentResp {
    public int responseCode;
    public String message;
    public String reference;
}
