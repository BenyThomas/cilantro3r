package com.DTO.mx;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@ToString
public class MxMessageRequest {
    private String sender;
    private String receiver;
    private String priority;
    private String service;
    private String clearingSystem;
    private String servicePrtry;
    private String choicePrtry;
    private String receiverDn;
    private String purposeCode;
    private String messageIdentifier;
    private String requestSubType;
    private String receiverBic;
    private String settlementMethod;
    private List<Txn> txnList;
}
