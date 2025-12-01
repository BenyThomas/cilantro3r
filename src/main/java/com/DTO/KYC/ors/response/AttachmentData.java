package com.DTO.KYC.ors.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AttachmentData {
    @JsonProperty("attachment_id")
    private String attachmentId;
    @JsonProperty("attachment_type")
    private String attachmentType;
    @JsonProperty("file_name")
    private String fileName;
    @JsonProperty("file_size")
    private String fileSize;
    @JsonProperty("file_type")
    private String fileType;
    @JsonProperty("mongo_db_id")
    private String mongoDbId;
    @JsonProperty("create_date")
    private String createDate;
    @JsonProperty("local_attachment_id")
    private String localAttachmentId;

    @Override
    public String toString() {
        return "AttachmentData{" +
                "attachmentId='" + attachmentId + '\'' +
                ", attachmentType='" + attachmentType + '\'' +
                ", fileName='" + fileName + '\'' +
                ", fileSize='" + fileSize + '\'' +
                ", fileType='" + fileType + '\'' +
                ", mongoDbId='" + mongoDbId + '\'' +
                ", createDate='" + createDate + '\'' +
                '}';
    }
}
