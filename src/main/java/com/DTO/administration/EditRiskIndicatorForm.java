package com.DTO.administration;

import lombok.Data;
import lombok.ToString;

import java.util.Date;

@Data
@ToString
public class EditRiskIndicatorForm {
    private Long id;
    private String branchCode;
    private Long indicatorId;
    private String valueType;
    private String value;
    private String updatedBy;
    private Date updatedDate;
}
