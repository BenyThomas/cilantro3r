package com.DTO.psssf;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PensionStatement {
        @JsonProperty("PMONTH")
        public String PMONTH;
        @JsonProperty("PYEAR")
        public String PYEAR;
        @JsonProperty("MONTHLY_PENSION")
        public String MONTHLY_PENSION;
        @JsonProperty("ARREARS")
        public String ARREARS;
        @JsonProperty("BANK_ACCOUNT_NO")
        public String BANK_ACCOUNT_NO;
        @JsonProperty("NO_OF_MONTHS")
        public String NO_OF_MONTHS;
        @JsonProperty("AMOUNT_PAID")
        public String AMOUNT_PAID;
}
