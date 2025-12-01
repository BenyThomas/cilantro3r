package com.models.kyc.ors;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "Brela_Response")
@Data
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ORSResponseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;
    @JsonProperty("success")
    @Column(name = "Status")
    private boolean success;
    @JsonProperty("message")
    @Column(name = "message")
    private String message;
    @JsonProperty("resultcode")
    @Column(name = "result_code")
    private int resultCode;
    @CreatedDate
    @Column(name = "CREATED_AT", updatable = false, nullable = false)
    @JsonIgnore
    private LocalDateTime createdAt;
    @LastModifiedDate
    @Column(name = "Updated_At", nullable = false)
    @JsonIgnore
    private LocalDateTime updatedAt;

    @Column(name = "certificate", nullable = false)
    private String certificate;
}
