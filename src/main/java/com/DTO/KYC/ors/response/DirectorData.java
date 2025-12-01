package com.DTO.KYC.ors.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DirectorData {
    @JsonProperty("__INDEX")
    private String index;
    @JsonProperty("type_of_person")
    private String typeOfPerson;
    @JsonProperty("country")
    private String country;
    @JsonProperty("origin_natural_person")
    private String originNaturalPerson;
    @JsonProperty("birth_date")
    private String birthDate;
    @JsonProperty("national_id")
    private String nationalId;
    @JsonProperty("middle_name")
    private String middle_name;
    @JsonProperty("first_name")
    private String firstName;
    @JsonProperty("last_name")
    private String lastName;
    @JsonProperty("gender")
    private String gender;
    @JsonProperty("nationality")
    private String nationality;
    @JsonProperty("tin")
    private String tin;
    @JsonProperty("tin_checked")
    private String tinChecked;
    @JsonProperty("tin_verif_data")
    private String tinVerifyData;
    @JsonProperty("type_of_local")
    private String typeOfLocal;
    @JsonProperty("region")
    private String region;
    @JsonProperty("distrinct")
    private String district;
    @JsonProperty("ward")
    private String ward;
    @JsonProperty("street")
    private String street;
    @JsonProperty("postcode")
    private String postCode;
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

    public DirectorData(String firstName, String lastName, String gender, String mobPhone) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.gender = gender;
        this.mobPhone = mobPhone;
    }
}
