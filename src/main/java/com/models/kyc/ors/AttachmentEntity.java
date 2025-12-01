package com.models.kyc.ors;

import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "brela_attachment")
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AttachmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonProperty("attachment_id")
    @Column(name = "attachment_id")
    private String attachmentId;
    @Column(name = "registration_number")
    private String registrationNumber;
    @JsonProperty("attachment_type")
    @Column(name = "attachment_type")
    private String attachmentType;
    @JsonProperty("file_name")
    @Column(name = "file_name")
    private String fileName;
    @JsonProperty("file_size")
    @Column(name = "file_size")
    private String fileSize;
    @JsonProperty("file_type")
    @Column(name = "file_type")
    private String fileType;
    @JsonProperty("mongo_db_id")
    @Column(name = "mongo_db_id")
    private String mongoDbId;
    @JsonProperty("local_attachment_id")
    private String localAttachmentId;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private Date createdAt;
    @LastModifiedDate
    @Column(name = "Updated_At", nullable = false)
    private Date updatedAt;

}
