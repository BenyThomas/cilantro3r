package com.DTO.administration;

import lombok.Data;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
@ToString
public class NewRiskIndicatorForm {
    private String branchCode;
    private Long indicatorId;
    private String valueType;
    private String value;
    private String createdBy;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date createdDate;
}
