/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.entities;

import lombok.*;

/**
 *
 * @author arthur.ndossi
 */
@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PUBLIC)
@NoArgsConstructor
@ToString
public class CreateAccountResponse {
    private String reference;
    private String responseCode;
    private String message;
    private String customerRim;
    private String accountNo;
}
