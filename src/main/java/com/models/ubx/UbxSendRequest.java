package com.models.ubx;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import jakarta.persistence.*;

@Data
@Entity
@Table(name = "Ubx_Sent_Request")
public class UbxSendRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JsonProperty("lastName")
    private String lastName;

    @JsonProperty("cif")
    private String cif;

    @JsonProperty("firstName")
    private String firstName;

    @JsonProperty("nationalId")
    private String nationalId;

    @JsonProperty("accountType")
    private String accountType;

    @JsonProperty("msisdn")
    private String msisdn;

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("cardNumber")
    private String cardNumber;

    @JsonProperty("channelId")
    private String channelId;

    @JsonProperty("customerName")
    private String customerName;

    @Column(name = "response_code")
    private String responseCode;
    @Column(name = "response", length = 3000)
    private String response;
    @Column(name = "Endpoint")
    private String endpoint;
    @Column(name = "UserIdentification")
    private String userIdentification;
    @Column(name = "institutionId")
    private String institutionId;

    @Column(name = "customerId")
    private String customerId;

    @Column(name = "userAction")
    private String userAction;

    @Column(name = "SavedToCBS", length = 3000)
    private String SavedToCBS;
}
