package com.DTO.Reports;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChargedOffDTO {
    @JsonProperty("accountNo")
    private String accountNumber;
    private String customerName;
    private String branchName;
    private String currentSystemClass;
    @JsonProperty("daysInArea")
    private int daysInArrears;
    @JsonProperty("provisionRate")
    private BigDecimal provisionRate;
    @JsonProperty("disbursementAmount")
    private BigDecimal disbursementAmount;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date disbursementDate;
    private String loanReference;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date maturityDate;
    private String phoneNumber;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date chargedOffDate;
    private BigDecimal totalChargedOffAmount;
    @JsonProperty( value = "phoneNo")
    private String phoneNo;

    public String toJson() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            log.error("Error While converting to Json: {}", e.getMessage());
            return "{}";  // Return empty JSON object in case of error
        }
    }

    @Override
    public String toString() {
        return "ChargedOff{" +
                "accountNo='" + accountNumber + '\'' +
                ", customerName='" + customerName + '\'' +
                ", branchName='" + branchName + '\'' +
                ", currentSystemClass='" + currentSystemClass + '\'' +
                ", daysInArrears=" + daysInArrears +
                ", disbursementAmount=" + disbursementAmount +
                ", disbursementDate=" + disbursementDate +
                ", loanReference='" + loanReference + '\'' +
                ", maturityDate=" + maturityDate +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", chargedOffDate=" + chargedOffDate +
                ", totalChargedOffAmount=" + totalChargedOffAmount +
                '}';
    }
}
