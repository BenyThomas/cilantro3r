package com.models.kyc.ors;

import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "brela_shareholder_shares")
@Data
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShareholderSharesEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @JsonIgnore
    private Long id;
    @JsonProperty("shareholder_item")
    @Column(name ="shareholder_item")
    private String shareholderItem;
    @JsonProperty("shareholder_name")
    @Column(name ="shareholder_name")
    private String shareholderName;
    @JsonProperty("share_class_tbl")
    @Column(name ="share_class_tbl")
    private String shareClassTbl;
    @JsonProperty("number_of_shares")
    @Column(name ="number_of_shares")
    private String shareNumberOfShares;
    @Column(name = "certificate")
    @JsonIgnore
    private String certificate;
    @CreatedDate
    @Column(name = "CREATED_AT", updatable = false, nullable = false)
    @JsonIgnore
    private LocalDateTime createdAt;
    @LastModifiedDate
    @Column(name = "Updated_At", nullable = false)
    @JsonIgnore
    private LocalDateTime updatedAt;
}
