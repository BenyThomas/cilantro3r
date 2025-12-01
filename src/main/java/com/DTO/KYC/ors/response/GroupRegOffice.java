package com.DTO.KYC.ors.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.EntityListeners;
import java.time.LocalDateTime;

@Data
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GroupRegOffice {
    @JsonProperty("country")
    private String country;
    @JsonProperty("type_of_local")
    private String typeOfLocal;
    @JsonProperty("region")
    private String region;
    @JsonProperty("distrinct")
    private String district;
    @JsonProperty("ward")
    private String ward;
    @JsonProperty("postcode")
    private String postCode;
    @JsonProperty("box")
    private String box;
    @JsonProperty("street")
    private String street;
    @JsonProperty("road")
    private String road;
    @JsonProperty("plot_number")
    private String plotNumber;
    @JsonProperty("block_number")
    private String blockNumber;
    @JsonProperty("house_number")
    private String houseNumber;
    @JsonProperty("email")
    private String email;
    @JsonProperty("mob_phone")
    private String mobPhone;


}
