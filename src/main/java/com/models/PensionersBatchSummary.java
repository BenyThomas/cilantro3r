package com.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Getter
@Setter
@Entity
@ToString
@Table(name = "pensioners_batch_summary")
public class PensionersBatchSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String reference;

    @Column(name = "batchDate")
    private Timestamp batchDate;

    @Column(name = "institutionName")
    private String institutionName;

    @Column(name = "batchDescription")
    private String batchDescription;

    @Column(name = "noOfTxns")
    private int noOfTxns;

    @Column(name = "totalAmount", precision = 65, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "created_by")
    private String createdBy;

    private String status;

    @Column(name = "cbs_status")
    private String cbsStatus;

    @Column(name = "create_dt")
    private String createDt;

    @Column(name = "verified_by")
    private String verifiedBy;

    @Column(name = "verified_dt")
    private Timestamp verifiedDt;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "approved_dt")
    private Timestamp approvedDt;

    @Column(name = "tiss_ref")
    private String tissRef;
}
