package com.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.util.Date;


/*
For TISS and EFT
 */
@Entity
@Table(name = "allowed_stp_account")
@Setter
@Getter
@ToString
public class AllowedSTPAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "acct_no", nullable = false, length = 255)
    private String acctNo;

    @Column(name = "acct_name", length = 255)
    private String acctName;

    @Column(name = "log_ts")
    private Date createDt = new Date();

    @Column(name = "status", length = 255)
    private String status;



}
