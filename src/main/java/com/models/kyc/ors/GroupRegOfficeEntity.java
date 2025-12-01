package com.models.kyc.ors;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "brela_group_reg_office")
@NoArgsConstructor
@AllArgsConstructor
@Data
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GroupRegOfficeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonIgnore
    private Long id;
    @JsonProperty("country")
    @Column(name = "COUNTRY")
    private String country;
    @JsonProperty("type_of_local")
    @Column(name = "TYPE_OF_LOCAL")
    private String typeOfLocal;
    @JsonProperty("Region")
    @Column(name = "REGION")
    private String region;
    @JsonProperty("district")
    @Column(name = "DISTRICT")
    private String district;
    @JsonProperty("ward")
    @Column(name = "WARD")
    private String ward;
    @JsonProperty("postcode")
    @Column(name = "POSTCODE")
    private String postcode;
    @JsonProperty("box")
    @Column(name = "BOX")
    private String box;
    @JsonProperty("street")
    @Column(name = "STREET")
    private String street;
    @JsonProperty("road")
    @Column(name = "ROAD")
    private String road;
    @JsonProperty("plot_number")
    @Column(name = "PLOT_NUMBER")
    private String plotNumber;
    @JsonProperty("block_number")
    @Column(name = "BLOCK_NUMBER")
    private String blockNumber;
    @JsonProperty("house_number")
    @Column(name = "HOUSE_NUMBER")
    private String houseNumber;
    @JsonProperty("email")
    @Column(name = "EMAIL")
    private String email;
    @JsonProperty("mob_phone")
    @Column(name = "MOB_PHONE")
    private String mobPhone;
    @Column(name = "certificate")
    private String certificate;
    @CreatedDate
    @Column(name = "CREATED_AT", updatable = false, nullable = false)
    @JsonIgnore
    private Date createdAt;
    @LastModifiedDate
    @Column(name = "Updated_At", nullable = false)
    @JsonIgnore
    private Date updatedAt;

}
