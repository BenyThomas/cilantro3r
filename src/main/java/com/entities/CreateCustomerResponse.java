package com.entities;

import lombok.*;

@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PUBLIC)
@NoArgsConstructor
@ToString
public class CreateCustomerResponse {
    private String reference;
    private String responseCode;
    private String message;
    private String customerRim;
    private String accountNo;
    private String trackNo;
}