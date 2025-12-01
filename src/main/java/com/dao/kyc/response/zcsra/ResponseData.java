package com.dao.kyc.response.zcsra;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ResponseData {
    @JsonProperty("RESPONSE_ID")
    private int responseId;

    @JsonProperty("PRSN_ID")
    private int prsnId;

    @JsonProperty("PRSN_LAST_NAME")
    private String prsnLastName;

    @JsonProperty("PRSN_FIRST_NAME")
    private String prsnFirstName;

    @JsonProperty("PRSN_SEX")
    private String prsnSex;

    @JsonProperty("PRSN_NATIONALITY_IND")
    private String prsnNationalityInd;

    @JsonProperty("PRSN_BIRTH_DATE")
    private String prsnBirthDate;

    @JsonProperty("PRSN_BIRTH_PLACE")
    private String prsnBirthPlace;

    @JsonProperty("PRSN_BIRTH_NATIONALITY")
    private String prsnBirthNationality;

    @JsonProperty("PRSN_EYE_COLOR")
    private String prsnEyeColor;

    @JsonProperty("PRSN_HEIGHT")
    private double prsnHeight;

    @JsonProperty("PRSN_PROOF_OF_BIRTH_TYPE")
    private String prsnProofOfBirthType;

    @JsonProperty("PRSN_OCCUPATION")
    private String prsnOccupation;

    @JsonProperty("PRSN_RES_ADDRESS")
    private String prsnResAddress;

    @JsonProperty("PRSN_RES_DISTRICT")
    private String prsnResDistrict;

    @JsonProperty("PRSN_RES_WARD")
    private String prsnResWard;

    @JsonProperty("PRSN_RES_TOWN_VILLAGE")
    private String prsnResTownVillage;

    @JsonProperty("PRSN_RES_HOUSE_PLOT")
    private String prsnResHousePlot;

    @JsonProperty("PRSN_POST_ADDRESS")
    private String prsnPostAddress;

    @JsonProperty("PRSN_POST_TOWN_VILLAGE")
    private String prsnPostTownVillage;

    @JsonProperty("PRSN_POST_CODE")
    private String prsnPostCode;

    @JsonProperty("PRSN_PHOTO")
    private String prsnPhoto;

    @JsonProperty("PRSN_KIN_TYPE")
    private String prsnKinType;

    @JsonProperty("PRSN_KIN_NAME")
    private String prsnKinName;

    @JsonProperty("PRSN_KIN_ADDRESS")
    private String prsnKinAddress;

    @JsonProperty("PRSN_KIN_PHONE")
    private String prsnKinPhone;

    @JsonProperty("PRSN_SPOUSE_ID")
    private int prsnSpouseId;

    @JsonProperty("PRSN_SPOUSE_FULL_NAME")
    private String prsnSpouseFullName;

    @JsonProperty("PRSN_FATHER_FULL_NAME")
    private String prsnFatherFullName;

    @JsonProperty("PRSN_FATHER_NATIONALITY")
    private String prsnFatherNationality;

    @JsonProperty("PRSN_FATHER_BIRTH_NATIONALITY")
    private String prsnFatherBirthNationality;

    @JsonProperty("PRSN_FATHER_BIRTH_PLACE")
    private String prsnFatherBirthPlace;

    @JsonProperty("PRSN_FATHER_ALIVE")
    private boolean prsnFatherAlive;

    @JsonProperty("PRSN_MOTHER_FULL_NAME")
    private String prsnMotherFullName;

    @JsonProperty("PRSN_MOTHER_NATIONALITY")
    private String prsnMotherNationality;

    @JsonProperty("PRSN_MOTHER_BIRTH_NATIONALITY")
    private String prsnMotherBirthNationality;

    @JsonProperty("PRSN_MOTHER_ALIVE")
    private boolean prsnMotherAlive;

    @JsonProperty("PRSN_MOTHER_BIRTH_PLACE")
    private String prsnMotherBirthPlace;

    @JsonProperty("PRSN_FEATURES")
    private String prsnFeatures;

    @JsonProperty("PRSN_MARITAL_STATUS")
    private String prsnMaritalStatus;

    @JsonProperty("PRSN_LIVING_TIME_IN_ZAN")
    private String prsnLivingTimeInZan;

    @JsonProperty("PRSN_POST_COUNTRY")
    private String prsnPostCountry;

    @JsonProperty("PRSN_PERSON_ID_NUM")
    private String prsnPersonIdNum;

    @JsonProperty("PRSN_EMAILS")
    private String prsnEmails;

    @JsonProperty("PRSN_PLACE_SHEHIA")
    private String prsnPlaceShehia;

    @JsonProperty("PRSN_SIGNATURE")
    private String prsnSignature;
}
