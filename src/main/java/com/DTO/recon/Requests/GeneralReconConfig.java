package com.DTO.recon.Requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class GeneralReconConfig {
        private String glAcct;
        private String txnType;
        private String tType;
        private String currency;
        private String datasourceUrl;
        private String datasourceUsername;
        private String datasourcePassword;
        private String datasourceDriver;
        private String datasourceQuery;
        private String datasourceCBSUrl;
        private String datasourceCBSUsername;
        private String datasourceCBSPassword;
        private String datasourceCBSDriver;
        private String datasourceCBSQuery;
        private String thirdPartyAcct;
}
