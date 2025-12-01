package com.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;

@Setter
@Getter
@ToString
@Entity
@Table(name = "pensioners_payroll")
public class PensionersPayroll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "trackingNo")
    private String trackingNo;

    @Column(name = "institution_id")
    private String institutionId;

    private String name;

    @Column(name = "cbs_name")
    private String cbsName;

    @Column(name = "percentage_match")
    private int percentageMatch;

    private String currency;

    private BigDecimal amount;

    private String account;

    @Column(name = "channel_identifier")
    private String channelIdentifier;

    @Column(name = "bankCode")
    private String bankCode;

    @Column(name = "pensioner_id")
    private String pensionerId;

    @Column(name = "batchReference")
    private String batchReference;

    @Column(name = "bankReference")
    private String bankReference;

    private String description;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "create_dt")
    private Date createDt;

    private String status;

    @Column(name = "responseCode")
    private String responseCode;

    private String message;

    private String comments;

    @Column(name = "cbs_status")
    private String cbsStatus;

    @Column(name = "verified_by")
    private String verifiedBy;

    @Column(name = "verified_dt")
    private Timestamp verifiedDt;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "approved_dt")
    private Timestamp approvedDt;

    @Column(name = "od_loan_status")
    private int odLoanStatus;

    @Column(name = "payroll_month")
    private int payrollMonth;

    @Column(name = "payroll_year")
    private int payrollYear;


    @Column(name = "sub_batch_reference")
    private String subBatchReference;

}
