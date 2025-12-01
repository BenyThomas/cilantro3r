package com.models.kyc.ors;

import com.fasterxml.jackson.annotation.JsonBackReference;
import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@Entity
@Table(name = "brela_group_share_capital")
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GroupShareCapitalEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID")
    @JsonIgnore
    private Long id;
    @JsonProperty("share_type")
    @Column(name = "SHARE_TYPE")
    private String shareType;
    @JsonProperty("total")
    @Column(name = "TOTAL")
    private String total;
    @JsonProperty("currency")
    @Column(name = "CURRENCY")
    private String currency;
    @JsonIgnore
    @Column(name = "certificate")
    private String certificate;
    @CreatedDate
    @Column(name = "CREATED_AT", updatable = false, nullable = false)
    @JsonIgnore
    private Date createdAt;
    @LastModifiedDate
    @Column(name = "Updated_At", nullable = false)
    @JsonIgnore
    private Date updatedAt;
}
