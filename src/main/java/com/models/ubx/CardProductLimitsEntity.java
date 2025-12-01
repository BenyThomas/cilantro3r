package com.models.ubx;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import jakarta.persistence.*;

@Data
@Entity
@Table(name = "visa_card_product_limits")
public class CardProductLimitsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonProperty("goodsNumberOfTransactionsLimit")
    private String goodsNumberOfTransactionsLimit;

    @JsonProperty("goodsLimit")
    private String goodsLimit;

    @JsonProperty("cashNumberOfTransactionsLimit")
    private String cashNumberOfTransactionsLimit;

    @JsonProperty("cashLimit")
    private String cashLimit;

    @JsonProperty("cardNotPresentlimit")
    private String cardNotPresentLimit;

    @JsonProperty("DepositAvailableLimit")
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
}
