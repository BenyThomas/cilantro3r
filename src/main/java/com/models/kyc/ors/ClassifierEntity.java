package com.models.kyc.ors;

import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@Table(name = "brela_classifier")
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClassifierEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long locallyUniqueId;
    @Column(name = "ID")
    private String id;

    @Column(name = "name")
    private String name;

    @Column(name = "alias")
    private String alias;
    @Column(name = "error_code")
    private String errorCode;
    @Column(name = "certificate", nullable = false)
    private String certificate;
    @CreatedDate
    @Column(name = "CREATED_AT", updatable = false, nullable = false)
    private Date createdAt;
    @LastModifiedDate
    @Column(name = "Updated_At", nullable = false)
    private Date updatedAt;
}
