package com.DTO.KYC.ors.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GroupCompanyData {
    @JsonProperty("incorporation_number")
    private String incorporationNumber;
    @JsonProperty("company_name")
    private String companyName;
    @JsonProperty("incorporation_date")
    private String incorporationDate;
    @JsonProperty("company_type")
    private String companyType;
    @JsonProperty("accounting_date")
    private String accountingDate;
    @JsonProperty("tax_payer_tin")
    private String taxPayerTin;
    @JsonProperty("name_reserved")
    private String nameReserved;
    @JsonProperty("state")
    private String state;
}
