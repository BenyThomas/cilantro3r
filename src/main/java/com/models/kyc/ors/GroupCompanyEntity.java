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
@Table(name = "brela_group_company")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GroupCompanyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID")
    @JsonIgnore
    private Long id;
    @JsonProperty("incorporation_number")
    @Column(name = "INCORPORATION_NUMBER")
    private String incorporationNumber;
    @JsonProperty("company_name")
    @Column(name = "COMPANY_NAME")
    private String companyName;
    @JsonProperty("incorporation_date")
    @Column(name = "INCORPORATION_DATE")
    private String incorporationDate;
    @JsonProperty("company_type")
    @Column(name = "COMPANY_TYPE")
    private String companyType;
    @JsonProperty("accounting_date")
    @Column(name = "ACCOUNTING_DATE")
    private String accountingDate;
    @JsonProperty("tax_payer_tin")
    @Column(name = "TAX_PAYER_TIN")
    private String taxPayerTin;
    @JsonProperty("name_reserved")
    @Column(name = "NAME_RESERVED")
    private String nameReserved;
    @JsonProperty("state")
    @Column(name = "STATE")
    private String state;
    @Column(name = "certificate", unique = true, nullable = false)
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
