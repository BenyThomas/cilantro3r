package com.models.kyc.ors;

import com.fasterxml.jackson.annotation.JsonBackReference;
import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "brela_issued_share_capital")
@Data
@EntityListeners(AuditingEntityListener.class)
@ToString
public class IssuedShareCapitalEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @JsonIgnore
    private Long id;
    @JsonProperty("ordinary")
    @Column(name = "ordinary")
    private String ordinary;
    @JsonProperty("currency")
    @Column(name ="currency")
    private String currency;
    @JsonProperty("__INDEX")
    @Column(name ="__INDEX")
    private String index;
    @JsonProperty("no_of_shares_issued")
    @Column(name ="no_of_shares_issued")
    private int noOfSharesIssued;
    @JsonProperty("value")
    @Column(name ="value")
    private double value;
    @JsonProperty("aggregate_nominal_value")
    @Column(name ="aggregate_nominal_value")
    private String aggregateNominalValue;
    @Column(name = "certificate")
    @JsonIgnore
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
