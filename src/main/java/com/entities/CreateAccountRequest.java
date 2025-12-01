/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import jakarta.validation.constraints.NotBlank;

/**
 *
 * @author arthur.ndossi
 */
@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PUBLIC)
@NoArgsConstructor
@ToString
public class CreateAccountRequest {

    @NotBlank(message = "Name is customerNumber")
    private String customerNumber;
    @NotBlank(message = "Name is productId")
    private String productId;
    @NotBlank(message = "Name is accountTitle")
    private String accountTitle;
    private String campaignRefId;
    private String openingReasonId;
    private String sourceOfFundId;
    private String monthlyIncome;
    @JsonIgnore
    private String branchCode;
    @JsonIgnore
    private String branchId;
    @JsonIgnore
    private String branchName;
    @NotBlank(message = "Name is connectToMobileChannel")
    private String connectToMobileChannel;

}
