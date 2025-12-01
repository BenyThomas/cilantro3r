package com.DTO.KYC.ors.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CompanyData {
    @JsonProperty("cert_number")
    private String certNumber;
    @JsonProperty("legal_name")
    private String legalName;
    @JsonProperty("company_type")
    private String companyType;
    @JsonProperty("company_subtype")
    private String companySubtype;
    @JsonProperty("incorporation_date")
    private String incorporationDate;
    @JsonProperty("reg_date")
    private String regDate;
    @JsonProperty("ba_category")
    private String baCategory;
    @JsonProperty("ba_division")
    private String baDivision;
    @JsonProperty("ba_group")
    private String baGroup;
    @JsonProperty("ba_class")
    private String baClass;
    @JsonProperty("diss_date")
    private String dissDate;
    @JsonProperty("CMDetailedInfo")
    private CMDetailedInfoData CMDetailedInfo;
    @JsonProperty("Signature")
    private String Signature;
}
