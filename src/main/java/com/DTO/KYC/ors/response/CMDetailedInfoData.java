package com.DTO.KYC.ors.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.helper.BusinessActivityDeserializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CMDetailedInfoData {
    @JsonProperty("candidate_user")
    private String candidateUser;
    @JsonProperty("send_to_tra")
    private String sendToTra;
    @JsonProperty("list_directors")
    private List<DirectorData> directors;
    @JsonProperty("last_save_user")
    private String lastSaveUser;
    @JsonProperty("submission_mode")
    private String submissionMode;
    @JsonProperty("username")
    private String username;
    @JsonProperty("is_final_decision")
    private Boolean isFinalDecision;
    @JsonProperty("submit_date")
    private String submitDate;
    @JsonProperty("islocallysaved")
    private String isLocallySaved;
    @JsonProperty("complete_date")
    private Date completeDate;
    @JsonProperty("applicant")
    private String applicant;
    @JsonProperty("name_reservation_in_use")
    private String nameReservationInUse;
    @JsonProperty("email")
    private String email;
    @JsonProperty("decision_date")
    private String decisionDate;
    @JsonProperty("list_shareholders")
    private List<ShareholderData> shareholders;
    @JsonProperty("list_issued_share_capital")
    private IssuedShareCapitalData issuedShareCapitals;
    @JsonProperty("services_requested")
    private String servicesRequested;
    @JsonProperty("is_paid")
    private String isPaid;
    @JsonProperty("create_user")
    private String createUser;
    @JsonProperty("service_name")
    private String serviceName;
    @JsonProperty("group_company_secretary")
    private GroupCompanySecretaryData groupCompanySecretaries;
    @JsonProperty("group_share_capital")
    private GroupShareCapitalData groupShareCapital;
    @JsonProperty("applicant_mobil_no")
    private String applicantMobilNo;
    @JsonProperty("entity_name")
    private String entityName;
    @JsonProperty("create_date")
    private String createDate;
    @JsonProperty("main_app_status_id")
    private String mainAppStatusId;
    @JsonProperty("group_company_info")
    private GroupCompanyData groupCompanyInfo;
    @JsonProperty("top_proc_instance_id")
    private String topProcInstanceId;
    @JsonProperty("last_save_date")
    private String lastSaveDate;
    @JsonProperty("group_reg_office")
    private GroupRegOffice groupRegOffice;
    @JsonProperty("services")
    private String services;
    @JsonProperty("entity_type")
    private String entityType;
    @JsonProperty("phone_mobile")
    private String phoneMobile;
    @JsonProperty("group_change_type")
    private String groupChangeType;
    @JsonProperty("group_charge_types")
    private String groupChangeTypes;
    @JsonProperty("group_winding_up")
    private String groupWindingUp;
    @JsonProperty("tm_filing_number")
    private String tmFilingNumber;
    @JsonProperty("tm_filing_name")
    private String tmFilingName;
    @JsonProperty("list_shareholder_shares")
    private List<ShareholderSharesData> shareholderShares;
    @JsonProperty("list_business_activitys")
    @JsonDeserialize(using = BusinessActivityDeserializer.class)
    private List<BusinessActivityData> businessActivities;
    @JsonProperty("object_type")
    private String objectType;
    @JsonProperty("tracking_no")
    private String trackingNo;
    @JsonProperty("create_date_frmt")
    private String createDateFrmt;
    @JsonProperty("error")
    private String error;



}
