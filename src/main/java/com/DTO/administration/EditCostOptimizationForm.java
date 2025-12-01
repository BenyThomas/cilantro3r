package com.DTO.administration;

import lombok.Data;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.util.Date;

@Data
@ToString
public class EditCostOptimizationForm {
    private Long id;
    private String branchCode;
    private Long serviceId;
    private BigDecimal cost;
    private String updatedBy;
    private Date updatedDate;
}
