package com.DTO.ubx;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CardLimits {

    @JsonProperty("goodsNumberOfTransactionsLimit")
    private String goodsNumberOfTransactionsLimit;

    @JsonProperty("goodsLimit")
    private String goodsLimit;

    @JsonProperty("cashNumberOfTransactionsLimit")
    private String cashNumberOfTransactionsLimit;

    @JsonProperty("cashLimit")
    private String cashLimit;

    @JsonProperty("cardNotPresentLimit")
    private String cardNotPresentLimit;

    @JsonProperty("depositAvailableLimit")
    private String depositAvailableLimit;

    @JsonProperty("transactionGoodsLimit")
    private String transactionGoodsLimit;

    @JsonProperty("transactionCashLimit")
    private String transactionCashLimit;

    @JsonProperty("transactionCardNotPresentLimit")
    private String transactionCardNotPresentLimit;

    @JsonProperty("paymentNumberOfTransactionsLimit")
    private String paymentNumberOfTransactionsLimit;

    @JsonProperty("paymentLimit")
    private String paymentLimit;

    @JsonProperty("transactionPaymentLimit")
    private String transactionPaymentLimit;

    // Getters and Setters
}
