/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.entities;

import jakarta.validation.constraints.NotBlank;

/**
 *
 * @author MELLEJI
 */
public class ReconTypeMappingForm {

    @NotBlank(message = "Please Select  Recon Type")
    private String reconType;
    @NotBlank(message = "Please select Report Type ")
    private String reportType;
    @NotBlank(message = "Please Enter the report display name ")
    private String reportName;

    public String getReconType() {
        return reconType;
    }

    public void setReconType(String reconType) {
        this.reconType = reconType;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getReportName() {
        return reportName;
    }

    public void setReportName(String reportName) {
        this.reportName = reportName;
    }

    @Override
    public String toString() {
        return "ReconTypeMappingForm{" + "reconType=" + reconType + ", reportType=" + reportType + ", reportName=" + reportName + '}';
    }
    

}
