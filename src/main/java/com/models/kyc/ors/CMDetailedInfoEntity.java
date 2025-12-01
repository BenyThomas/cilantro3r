package com.models.kyc.ors;

import com.fasterxml.jackson.annotation.*;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "brela_companies_details")
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class CMDetailedInfoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID")
    @JsonIgnore
    private Long id;
    @JsonProperty("candidate_user")
    @Column(name = "candidate_user")
    private String candidateUser;
    @JsonProperty("last_save_user")
    @Column(name = "last_save_user")
    private String lastSaveUser;
    @JsonProperty("services_requested")
    @Column(name = "services_requested")
    private String servicesRequested;
    @Column(name = "certificate", unique = true, nullable = false)
    private String certificate;
    @JsonProperty("send_to_tra")
    private String sendToTra;
    @JsonProperty("submission_mode")
    @Column(name ="submission_mode")
    private String submissionMode;
    @JsonProperty("username")
    @Column(name ="username")
    private String username;
    @JsonProperty("is_final_decision")
    @Column(name ="is_final_decision")
    private Boolean isFinalDecision;
    @JsonProperty("submit_date")
    @Column(name ="submit_date")
    private String submitDate;
    @JsonProperty("islocallysaved")
    @Column(name ="islocallysaved")
    private String isLocallySaved;
    @JsonProperty("complete_date")
    @Column(name ="complete_date")
    private Date completeDate;
    @JsonProperty( "applicant")
    @Column(name ="applicant")
    private String applicant;
    @JsonProperty( "name_reservation_in_use")
    @Column(name ="name_reservation_in_use")
    private String nameReservationInUse;
    @JsonProperty( "email")
    @Column(name ="email")
    private String email;
    @JsonProperty( "decision_date")
    @Column(name ="decision_date")
    private String decisionDate;
    @JsonProperty("is_paid")
    @Column(name ="is_paid")
    private String isPaid;
    @JsonProperty("create_user")
    @Column(name ="create_user")
    private String createUser;
    @JsonProperty("service_name")
    @Column(name ="service_name")
    private String serviceName;
    @JsonProperty( "applicant_mobil_no")
    @Column(name ="applicant_mobil_no")
    private String applicantMobilNo;
    @JsonProperty( "entity_name")
    @Column(name ="entity_name")
    private String entityName;
    @JsonProperty( "create_date")
    @Column(name ="create_date")
    private String createDate;
    @JsonProperty( "main_app_status_id")
    @Column(name ="main_app_status_id")
    private String mainAppStatusId;
    @JsonProperty( "top_proc_instance_id")
    @Column(name ="top_proc_instance_id")
    private String topProcInstanceId;
    @JsonProperty( "last_save_date")
    @Column(name ="last_save_date")
    private String lastSaveDate;
    @JsonProperty( "services")
    @Column(name ="services")
    private String services;
    @JsonProperty( "entity_type")
    @Column(name ="entity_type")
    private String entityType;
    @JsonProperty( "phone_mobile")
    @Column(name ="phone_mobile")
    private String phoneMobile;
    @JsonProperty( "group_change_type")
    @Column(name ="group_change_type")
    private String groupChangeType;
    @JsonProperty( "group_charge_types")
    @Column(name ="group_change_types")
    private String groupChangeTypes;
    @JsonProperty( "group_winding_up")
    @Column(name ="group_winding_up")
    private String groupWindingUp;
    @JsonProperty( "tm_filing_number")
    @Column(name ="tm_filing_number")
    private String tmFilingNumber;
    @JsonProperty( "tm_filing_name")
    @Column(name ="tm_filing_name")
    private String tmFilingName;
    @CreatedDate
    @Column(name = "CREATED_AT", updatable = false, nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonIgnore
    private Date createdAt;
    @LastModifiedDate
    @Column(name = "Updated_At", nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonIgnore
    private Date updatedAt;
}