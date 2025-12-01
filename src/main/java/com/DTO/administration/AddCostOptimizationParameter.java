package com.DTO.administration;

import lombok.Data;
import lombok.ToString;

import javax.validation.constraints.NotBlank;

@Data
@ToString
public class AddCostOptimizationParameter {
    @NotBlank(message = "Please enter optimization parameter")
    public String parameter;
    public String createdBy;
    public String createdDate;
}
