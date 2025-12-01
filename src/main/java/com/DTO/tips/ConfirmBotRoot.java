package com.DTO.tips;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConfirmBotRoot {

    public String payerRef;
    public Payer payer;
    public Payee payee;
    public Amount amount;
    public EndUserFee endUserFee;
    public TransactionType transactionType;
    public String lookupId;
    public String transferState;
    public String transferstateReason;
    public String payeeRef;
    public String switchRef;
    public String transferDate;
    public String completedDate;
    public String description;
    public TransactionCategory transactionCategory;
    public SettlementWindow settlementWindow;
    public ErrorInformation errorInformation;

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
}
