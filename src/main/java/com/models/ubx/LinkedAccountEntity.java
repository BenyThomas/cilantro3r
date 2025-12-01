package com.models.ubx;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import jakarta.persistence.*;

@Data
@Entity
@Table(name = "visa_linked_accounts")
public class LinkedAccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonProperty("accountType")
    private String accountType;

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("indicator")
    private String indicator;
}
