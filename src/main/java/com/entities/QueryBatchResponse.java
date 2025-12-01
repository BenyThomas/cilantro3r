package com.entities;

import lombok.*;

import java.util.List;

@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PUBLIC)
@NoArgsConstructor
@ToString
public class QueryBatchResponse {
    private String result;
    private String reference;
    private String message;
    private String availableBalance;
    private String ledgerBalance;
    private String txnId;
    private QueryInfo queryInfo;
    private List<Txn> txns;
}
