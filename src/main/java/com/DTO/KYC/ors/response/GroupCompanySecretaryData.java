package com.DTO.KYC.ors.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GroupCompanySecretaryData {
    @JsonProperty("type_of_person")
    private String typeOfPerson;
    @JsonProperty("origin_natural_person")
    private String originNaturalPerson;
    @JsonProperty("birth_date")
    private String birthDate;
    @JsonProperty("national_id")
    private String nationalId;
    @JsonProperty("first_name")
    private String firstName;
    @JsonProperty("middle_name")
    private String middleName;
    @JsonProperty("last_name")
    private String lastName;
    @JsonProperty("gender")
    private String gender;
    @JsonProperty("nationality")
    private String nationality;
    @JsonProperty("email")
    private String email;
    @JsonProperty("mob_phone")
    private String mobPhone;
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
}
