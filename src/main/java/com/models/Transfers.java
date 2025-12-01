/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.models;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 *
 * @author melleji.mollel
 */
@Getter
@Setter
@ToString
@Entity
@Table(name = "transfers",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = "reference"),})
public class Transfers {

    @Getter(AccessLevel.PUBLIC)
    @Setter(AccessLevel.PUBLIC)
    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @Column(name = "message_type")
    public String message_type;
    @Column(name = "txn_type")
    public String txn_type;
    @Column(name = "sourceAcct")
    public String sourceAcct;
    @Column(name = "destinationAcct")
    public String destinationAcct;
    @Column(name = "amount")
    public String amount;
    @Column(name = "charge")
    public String charge;
    @Column(name = "currency")
    public String currency;
    @Column(name = "beneficiaryName")
    public String beneficiaryName;
    @Column(name = "beneficiaryBIC")
    public String beneficiaryBIC;
    @Column(name = "beneficiary_contact")
    public String beneficiary_contact;
    @Column(name = "senderBIC")
    public String senderBIC;
    @Column(name = "sender_phone")
    public String sender_phone;
    @Column(name = "sender_address")
    public String senderAddress;
    @Column(name = "sender_name")
    public String senderName;
    @Column(name = "reference", unique = false)
    public String reference;
    @Column(name = "txid")
    public String txid;
    @Column(name = "instrId")
    public String instrId;
    @Column(name = "batch_reference")
    public String batchReference;
    @Column(name = "batch_reference2")
    public String batch_reference2;
    @Column(name = "code")
    public String code;
    @Column(name = "supportingDocument")
    public String supportingDocument;
    @Column(name = "status")
    public String status;
    @Column(name = "response_code")
    public String responseCode;
    @Column(name = "comments")
    public String comments;
    @Column(name = "purpose")
    public String purpose;
    @Column(name = "swift_message")
    public String swiftMessage;
    @Column(name = "direction")
    public String direction;
    @Column(name = "originallMsgNmId")
    public String originallMsgNmId;
    @Column(name = "initiated_by")
    public String initiatedBy;
    @Column(name = "returned_by")
    public String returnedBy;
    @Column(name = "modified_by")
    public String modifiedBy;
    @Column(name = "branch_approved_by")
    public String branchApprovedBy;
    @Column(name = "hq_approved_by")
    public String hqApprovedBy;
    @Column(name = "value_date")
    public String valueDate;

    @Column(name = "create_dt")
//    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public String createDt;

    @Column(name = "modified_dt")
    public String modified_dt;
    @Column(name = "returned_dt")
    public String returnedDt;
    @Column(name = "branch_approved_dt")
    public String branchApprovedDt;
    @Column(name = "hq_approved_dt")
    public String hqApprovedDt;
    @Column(name = "branch_no")
    public String branchNo;
    @Column(name = "cbs_status")
    public String cbsStatus;
    @Column(name = "message")
    public String message;
    @Column(name = "callbackurl")
    public String callbackurl;
    @Column(name = "ibankstatus")
    public String ibankstatus;
    @Column(name = "units")
    public String units;

    @Column(name = "is_synced_pensions")
    public String isSyncedPensions;

    @CreationTimestamp
    @Column(name = "created_at")
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
