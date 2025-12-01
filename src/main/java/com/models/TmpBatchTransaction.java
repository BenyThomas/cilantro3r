package com.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "tmp_batch_transaction")
@Setter
@Getter
@ToString
public class TmpBatchTransaction {
//VisaCardBINConfiguration
    @Id
    @Column(name = "reference", nullable = false, length = 255)
    private String reference;

    @Column(name = "callbackUrl", length = 255)
    private String callbackUrl;

    @Column(name = "createDt")
    private Date createDt = new Date();

    @Column(name = "endRecId", length = 255)
    private int endRecId;

    @Column(name = "itemCount", length = 255)
    private int itemCount;

    @Column(name = "result", length = 255)
    private String result;

    @Column(name = "message", length = 255)
    private String message;

    @Column(name = "startRecId", length = 255)
    private int startRecId;

    @Column(name = "timestamp")
    private Date timestamp = new Date();

    @Column(name = "totalAmount", precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "updateDt")
    private Date updateDt = new Date();

    @Column(name = "completedDt")
    private Date completedDt = null;

    @Column(name = "successCount", nullable = false)
    private Long successCount;

    @Column(name = "failureCount", nullable = false)
    private Long failureCount;


}
