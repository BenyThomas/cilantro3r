package com.DTO.KYC.ors;

import com.DTO.KYC.ors.response.DirectorData;
import com.DTO.KYC.ors.response.ShareholderData;
import com.DTO.KYC.ors.response.ShareholderSharesData;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EntityInfoDTO {
    @JsonProperty("cert_number")
    private String regNo;
    @JsonProperty("legal_name")
    private String entityLegalName;
    @JsonProperty("list_directors")
    private List<DirectorData> directorDataList;
    @JsonProperty("list_shareholders")
    private List<ShareholderData> shareholderDataList;
    @JsonProperty("list_shareholder_shares")
    private List<ShareholderSharesData> shareholderSharesDataList;
}
