package com.models.ubx;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "customer_card_request")
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerCardRequest {
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
    @JsonProperty("username")
    private String currentUserName;
    @JsonProperty("branchCode")
    private String branchCode;
    @JsonProperty("panExpireDate")
    private String panExpireDate;
    @JsonProperty("email")
    private String email;
    @JsonIgnore
    private String charge;
    @JsonProperty("custId")
    private String custId;
    @JsonProperty("customerRim")
    private String customerRim;
    @JsonProperty("category")
    private String category;
}
