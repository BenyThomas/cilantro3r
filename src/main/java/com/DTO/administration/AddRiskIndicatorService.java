package com.DTO.administration;

import lombok.Data;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotBlank;

@Data
@ToString
public class AddRiskIndicatorService {
    @NotBlank(message = "Please enter indicator service name")
    public String service;
    @NotBlank(message = "Please enter indicator service description")
    public String description;
    public String createdBy;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    public String createdDate;
}
