package com.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "visa_card_bin_configuration")
@Setter
@Getter
@ToString
public class VisaCardBINConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ref_desc", nullable = false, length = 255)
    private String refDesc;

    @Column(name = "ref_key", length = 255)
    private String refKey;

    @Column(name = "log_ts")
    private Date createDt = new Date();

    @Column(name = "status", length = 255)
    private String status;



}
