package com.DTO.Reports;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioPositionReportDTO {
    @JsonProperty("branchName")
    private String branchName;
    @JsonProperty("policyNumber")
    private String policyNumber;
    @JsonProperty("customerName")
    private String customerName;
    @JsonProperty("checkNumber")
    private String checkNumber;
    @JsonProperty("accountNo")
    private String accountNo;
    @JsonProperty("loanAmount")
    private BigDecimal loanAmount;
    @JsonProperty("processingFee")
    private BigDecimal processingFee;
    @JsonProperty("interestRate")
    private BigDecimal interestRate;
    @JsonProperty("loanTerm")
    private Integer loanTerm;
    @JsonProperty("disbursedDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date disbursedDate;
    @JsonProperty("maturityDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date maturityDate;
    @JsonProperty("outstandingPrincipal")
    private BigDecimal outstandingPrincipal;
    @JsonProperty("outstandingInterest")
    private BigDecimal outstandingInterest;
    @JsonProperty("totalOutstandingLoan")
    private BigDecimal totalOutstandingLoan;
    @JsonProperty("daysInArrears")
    private Integer daysInArrears;
    @JsonProperty("phoneNumber")
    private String phoneNumber;
    @JsonProperty("loanTermId")
    private Long loanTermId;
    @JsonProperty("classificationStatus")
    private String classificationStatus;
    @JsonProperty("birthOfDate")
    private String birthOfDate;

}
