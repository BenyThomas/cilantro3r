package com.DTO.KYC.ors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AttachmentAccessDTO {
    private String filename;
    private String content;
    private String contentType;
    private long size;
    private String projectCode;
    private  String projectName;
    private String thirdPartId;
    private String entityRegNo;
}
