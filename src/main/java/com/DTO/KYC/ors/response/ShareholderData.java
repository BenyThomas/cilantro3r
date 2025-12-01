package com.DTO.KYC.ors.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShareholderData {
    @JsonProperty("person_uid")
    private String personUid;
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
    private String middleName;
    @JsonProperty("first_name")
    private String firstName;
    @JsonProperty("last_name")
    private String lastName;
    @JsonProperty("__INDEX")
    private String index;
    @JsonProperty("gender")
    private String gender;
    @JsonProperty("nationality")
    private String nationality;
    @JsonProperty("type_of_local")
    private String typeOfLocal;
    @JsonProperty("region")
    private String region;
    @JsonProperty("distrinct")
    private String district;
    @JsonProperty("ward")
    private String ward;
    @JsonProperty("postcode")
    private String postcode;
    @JsonProperty("email")
    private String email;
    @JsonProperty("mob_phone")
    private String mobPhone;

}
