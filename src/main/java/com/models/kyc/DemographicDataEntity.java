package com.models.kyc;




import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "DEMOGRAPHIC_DATA")
public class DemographicDataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "RESPONSE_ID", nullable = false)
    private int responseId;

    @Column(name = "PRSN_ID", nullable = false)
    private int prsnId;

    @Column(name = "PRSN_LAST_NAME")
    private String prsnLastName;

    @Column(name = "PRSN_FIRST_NAME")
    private String prsnFirstName;

    @Column(name = "PRSN_SEX")
    private String prsnSex;

    @Column(name = "PRSN_NATIONALITY_IND")
    private String prsnNationalityInd;

    @Column(name = "PRSN_BIRTH_DATE")
    private String prsnBirthDate;

    @Column(name = "PRSN_BIRTH_PLACE")
    private String prsnBirthPlace;

    @Column(name = "PRSN_BIRTH_NATIONALITY")
    private String prsnBirthNationality;

    @Column(name = "PRSN_EYE_COLOR")
    private String prsnEyeColor;

    @Column(name = "PRSN_HEIGHT")
    private double prsnHeight;

    @Column(name = "PRSN_PROOF_OF_BIRTH_TYPE")
    private String prsnProofOfBirthType;

    @Column(name = "PRSN_OCCUPATION")
    private String prsnOccupation;

    @Column(name = "PRSN_RES_ADDRESS")
    private String prsnResAddress;

    @Column(name = "PRSN_RES_DISTRICT")
    private String prsnResDistrict;

    @Column(name = "PRSN_RES_WARD")
    private String prsnResWard;

    @Column(name = "PRSN_RES_TOWN_VILLAGE")
    private String prsnResTownVillage;

    @Column(name = "PRSN_RES_HOUSE_PLOT")
    private String prsnResHousePlot;

    @Column(name = "PRSN_POST_ADDRESS")
    private String prsnPostAddress;

    @Column(name = "PRSN_POST_TOWN_VILLAGE")
    private String prsnPostTownVillage;

    @Column(name = "PRSN_POST_CODE")
    private String prsnPostCode;

    @Column(name = "PRSN_PHOTO", columnDefinition = "TEXT")
    private String prsnPhoto;

    @Column(name = "PRSN_KIN_TYPE")
    private String prsnKinType;

    @Column(name = "PRSN_KIN_NAME")
    private String prsnKinName;

    @Column(name = "PRSN_KIN_ADDRESS")
    private String prsnKinAddress;

    @Column(name = "PRSN_KIN_PHONE")
    private String prsnKinPhone;

    @Column(name = "PRSN_SPOUSE_ID")
    private int prsnSpouseId;

    @Column(name = "PRSN_SPOUSE_FULL_NAME")
    private String prsnSpouseFullName;

    @Column(name = "PRSN_FATHER_FULL_NAME")
    private String prsnFatherFullName;

    @Column(name = "PRSN_FATHER_NATIONALITY")
    private String prsnFatherNationality;

    @Column(name = "PRSN_FATHER_BIRTH_NATIONALITY")
    private String prsnFatherBirthNationality;

    @Column(name = "PRSN_FATHER_BIRTH_PLACE")
    private String prsnFatherBirthPlace;

    @Column(name = "PRSN_FATHER_ALIVE")
    private boolean prsnFatherAlive;

    @Column(name = "PRSN_MOTHER_FULL_NAME")
    private String prsnMotherFullName;

    @Column(name = "PRSN_MOTHER_NATIONALITY")
    private String prsnMotherNationality;

    @Column(name = "PRSN_MOTHER_BIRTH_NATIONALITY")
    private String prsnMotherBirthNationality;

    @Column(name = "PRSN_MOTHER_ALIVE")
    private boolean prsnMotherAlive;

    @Column(name = "PRSN_MOTHER_BIRTH_PLACE")
    private String prsnMotherBirthPlace;

    @Column(name = "PRSN_FEATURES")
    private String prsnFeatures;

    @Column(name = "PRSN_MARITAL_STATUS")
    private String prsnMaritalStatus;

    @Column(name = "PRSN_LIVING_TIME_IN_ZAN")
    private String prsnLivingTimeInZan;

    @Column(name = "PRSN_POST_COUNTRY")
    private String prsnPostCountry;

    @Column(name = "PRSN_PERSON_ID_NUM")
    private String prsnPersonIdNum;

    @Column(name = "PRSN_EMAILS")
    private String prsnEmails;

    @Column(name = "PRSN_PLACE_SHEHIA")
    private String prsnPlaceShehia;

    @Column(name = "PRSN_POST_STREET")
    private String prsnPostStreet;

    @Column(name = "PRSN_SIGNATURE", columnDefinition = "TEXT")
    private String prsnSignature;
}
