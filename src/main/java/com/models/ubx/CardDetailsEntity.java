package com.models.ubx;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "card")
public class CardDetailsEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_no")
    private String accountNo;

    @Column(name = "phone")
    private String phone;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "custid")
    private String custid;

    @Column(name = "customer_rim_no")
    private String customerRirmNo;

    @Column(name = "reference")
    private String reference;

    @Column(name = "customer_shortName")
    private String customerShortName;

    @Column(name = "customer_category")
    private String customerCategory;

    @Column(name = "address1")
    private String address1;

    @Column(name = "address2")
    private String address2;

    @Column(name = "address3")
    private String address3;
    @Column(name = "address4")
    private String address4;


    @Column(name = "status")
    private String status;

    @Column(name = "responseCode")
    private String responseCode;
    @Column(name = "PAN")
    private String pan;
    @Column(name = "stage")
    private String stage;
    @Column(name = "originating_branch")
    private String originatingBranch;
    @Column(name = "collecting_branch")
    private String collectingBranch;
    @Column(name = "create_dt")
    private LocalDateTime createdDt;
    @Column(name = "created_by")
    private String createdBy;
    @Column(name = "approver1")
    private String approver1;
    @Column(name = "approver2")
    private String approver2;
    @Column(name = "approver1_dt")
    private LocalDateTime approver1Dt;
    @Column(name = "approver2_dt")
    private LocalDateTime approver2Dt;
    @Column(name = "dispatched_by")
    private String dispatchedBy;
    @Column(name = "dispatched_dt")
    private LocalDateTime dispatchedDt;
    @Column(name = "hq_received_by")
    private String hqReceivedBy;
    @Column(name = "hq_received_dt")
    private LocalDateTime hqReceivedDt;
    @Column(name = "issued_by")
    private String issuedBy;
    @Column(name = "issued_dt")
    private LocalDateTime issuedDt;
    @Column(name = "cardexpire_dt")
    private String cardexpireDt;
    @Column(name = "received_from_printing")
    private String receivedFromPrinting;
    @Column(name = "received_from_printing_dt")
    private LocalDateTime receivedFromPrintingDt;
    @Column(name = "charge")
    private Integer charge;
    @Column(name = "is_charged")
    private boolean isCharged;
    @Column(name = "is_allowed")
    private String isAllowed="Y";

    @Column(name = "otac")
    private String otac;
    @Column(name = "newPin")
    private String newPin;
    @Column(name = "saved_to_cbs")
    private String savedToCbs;

    @Column(name = "channel_id")
    private String channelId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ubx_status")
    private UBXStatus ubxStatus;
    @Column(name = "bin")
    private String bin;
    @Column(name = "email")
    private String email;

    @Column(name = "national_id")
    private String nationalId;



}
