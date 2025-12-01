package com.DTO.KYC.ors.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShareholderSharesData {
    @JsonProperty("shareholder_item")
    private String shareholderItem;
    @JsonProperty("shareholder_name")
    private String shareholderName;
    @JsonProperty("share_class_tbl")
    private String shareClassTbl;
    @JsonProperty("number_of_shares")
    private String shareNumberOfShares;
}
