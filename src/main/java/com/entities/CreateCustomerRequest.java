package com.entities;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PUBLIC)
public class CreateCustomerRequest {

    private String firstName;
    private String middleName;
    private String lastName;
    private String nationality;
    private String gender;
    private String maritalStatus;
    private String dob;
    private String residenceRegion;
    private String residenceDistrict;
    private String residenceArea;
    private String residenceHouseNo;
    private String postalAddress;
    private String phoneNumber;
    private String emailAddress;
    private String idType;
    private String idNumber;
    private String photo;
    private String signature;
    private String city;
    private String title;
    private String customerTypeId;
    private String customerTypeCat;
    private String birthDistrict;
    private String birthPlace;
    private String birthRegion;
    private String birthWard;
    private String employer;
    private String employmentStatus;
    private String incomeSource;
    private String occupation;
    private String branchCode;
    private String branchId;
    private String branchName;
    private String monthlyIncome;
    private String chequeNumber;
    private String otherPhoneNumber;
    private Integer amlServiceId;
    private String openingReasonId;
    private Integer disabilityStatusId;
    private String businessActivityId;
    private Boolean isPE;

}

