package com.DTO.Reports;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.Date;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NisogezeReportRequestDTO {
    @JsonProperty("loanType")
    private String loanType;
    @JsonProperty(value = "fromDate")
    private String fromDate;
    @JsonProperty(value = "toDate")
    private String toDate;
}
