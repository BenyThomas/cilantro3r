package com.controller.tips;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

@Setter
@Getter
@ToString
public class TIPSTransferForm {

        @NotBlank(message = "Please Enter Sender account number")
        public String senderAccount;
        @NotBlank(message = "Please Enter Sender Name")
        public String senderName;
        @NotEmpty
        @Pattern(regexp = "^(\\d+(?:[\\.\\,]\\d{0,2})?)", message = "Invalid amount")
        public String amount;
        @NotBlank(message = "Please Select paying currency")
        public String currency;
        @NotBlank(message = "Please Enter Beneficiary account number")
        public String beneficiaryAccount;
        @NotBlank(message = "Please Enter Beneficiary Name")
        public String beneficiaryName;
        @NotBlank(message = "Please Select Beneficiary Bank")
        public String beneficiaryBIC;
        @NotBlank(message = "Please Enter Sender Address")
        public String senderAddress;
        @NotBlank(message = "Please Enter Beneficiary Contact")
        public String beneficiaryContact;
        @NotBlank(message = "Please Enter Sender Phone")
        public String senderPhone;
        @NotBlank(message = "Please Enter Payment purpose")
        public String description;
        public String intermediaryBank;
        public String currencyConversion;
        public String rubikonRate;
        public String requestingRate;
        public String batchReference;
        public String fxType;
        public String transactionType;
        public String senderBic;
        public String reference;
        public String relatedReference;
        public String transactionDate;
        public String messageType;
        public String swiftMessage;
        public String chargeDetails;
        public String channel;
        public String comments;
        public String message;
        public String responseCode;
        public String correspondentBic;

    }


