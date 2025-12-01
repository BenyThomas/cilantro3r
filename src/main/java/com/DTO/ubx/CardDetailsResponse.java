package com.DTO.ubx;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class CardDetailsResponse {

    @JsonProperty("customerId")
    private String customerId;

    @JsonProperty("cardNumber")
    private String cardNumber;

    @JsonProperty("cardStatus")
    private String cardStatus;

    @JsonProperty("cardExpiryDate")
    private String cardExpiryDate;

    @JsonProperty("cardHoldResponseCode")
    private String cardHoldResponseCode;

    @JsonProperty("cardProduct")
    private String cardProduct;

    @JsonProperty("dateIssued")
    private String dateIssued;

    @JsonProperty("dateActivated")
    private String dateActivated;

    @JsonProperty("serviceRestrictionCode")
    private String serviceRestrictionCode;

    @JsonProperty("contact")
    private String contact;

    @JsonProperty("contactless")
    private String contactless;

    @JsonProperty("linkedAccounts")
    private List<LinkedAccount> linkedAccounts;

    @JsonProperty("cardLimits")
    private CardLimits cardLimits;

    @JsonProperty("cardProductLimits")
    private CardProductLimits cardProductLimits;

}
