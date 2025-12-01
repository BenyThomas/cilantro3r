package com.models.kyc.ors;

import com.fasterxml.jackson.annotation.*;

import javax.persistence.*;

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
@Table(name = "brela_directors")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DirectorEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonIgnore
    private Long id;
    @JsonProperty("type_of_person")
    @Column(name = "TYPE_OF_PERSON")
    private String typeOfPerson;
    @JsonProperty("country")
    @Column(name = "COUNTRY")
    private String country;
    @JsonProperty("origin_natural_person")
    @Column(name = "ORIGIN_NATURAL_PERSON")
    private String originNaturalPerson;
    @JsonProperty("birth_date")
    @Column(name = "BIRTH_DATE")
    private String birthDate;
    @JsonProperty("national_id")
    @Column(name = "NATIONAL_ID")
    private String nationalId;
    @Column(name = "certificate",nullable = false)
    private String certificate;
    @JsonProperty("middle_name")
    @Column(name = "MIDDLE_NAME")
    private String middleName;
    @JsonProperty("first_name")
    @Column(name = "FIRST_NAME")
    private String firstName;
    @JsonProperty("last_name")
    @Column(name = "LAST_NAME")
    private String lastName;
    @JsonProperty("gender")
    @Column(name = "GENDER")
    private String gender;
    @JsonProperty("nationality")
    @Column(name = "NATIONALITY")
    private String nationality;
    @JsonProperty("tin")
    @Column(name = "TIN")
    private String tin;
    @JsonProperty("tin_checked")
    @Column(name = "TIN_CHECKED")
    private String tinChecked;
    @JsonProperty("tin_verif_data")
    @Column(name = "TIN_VERIFY_DATA")
    private String tinVerifyData;
    @JsonProperty("type_of_local")
    @Column(name = "TYPE_OF_LOCAL")
    private String typeOfLocal;
    @JsonProperty("region")
    @Column(name = "REGION")
    private String region;
    @JsonProperty("district")
    @Column(name = "DISTRICT")
    private String district;
    @JsonProperty("ward")
    @Column(name = "WARD")
    private String ward;
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
    @JsonProperty("postcode")
    @Column(name = "POSTCODE")
    private String postcode;
    @JsonProperty("email")
    @Column(name = "EMAIL")
    private String email;
    @JsonProperty("mob_phone")
    @Column(name = "MOB_PHONE")
    private String mobPhone;
    @JsonProperty("__INDEX")
    @Column(name = "__INDEX")
    private String index;
    @CreatedDate
    @Column(name = "CREATED_AT", updatable = false, nullable = false)
    @JsonIgnore
    private Date createdAt;
    @LastModifiedDate
    @Column(name = "Updated_At", nullable = false)
    @JsonIgnore
    private Date updatedAt;

}
