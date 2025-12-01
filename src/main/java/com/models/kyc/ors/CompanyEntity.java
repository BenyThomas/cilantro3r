package com.models.kyc.ors;



import com.fasterxml.jackson.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "brela_companies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CompanyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID")
    @JsonIgnore
    private Long id;
    @JsonProperty("cert_number")
    @Column(name = "cert_number",unique = true,nullable = false)
    private String certNumber;
    @JsonProperty("legal_name")
    @Column(name ="legal_name")
    private String legalName;
    @JsonProperty("company_type")
    @Column(name ="company_type")
    private String companyType;
    @JsonProperty("company_subtype")
    @Column(name ="company_subtype")
    private String companySubtype;
    @JsonProperty("incorporation_date")
    @Column(name ="incorporation_date")
    private String incorporationDate;
    @JsonProperty("reg_date")
    @Column(name ="reg_date")
    private String regDate;
    @JsonProperty("ba_category")
    @Column(name ="ba_category")
    private String baCategory;
    @JsonProperty("ba_division")
    @Column(name ="ba_division")
    private String baDivision;
    @JsonProperty("ba_group")
    @Column(name ="ba_group")
    private String baGroup;
    @JsonProperty("ba_class")
    @Column(name ="ba_class")
    private String baClass;
    @JsonProperty("diss_date")
    @Column(name ="diss_date")
    private String dissDate;
    @JsonProperty("Signature")
    @Column(name ="Signature", length = 3000)
    private String Signature;
    @CreatedDate
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    @JsonIgnore
    private Date createdAt;
    @LastModifiedDate
    @Column(name = "UPDATED_AT",nullable = false)
    @JsonIgnore
    private Date updatedAt;
    @JsonProperty("InformationAccessTime")
    @Column(name = "InformationAccessTime")
    private String informationAccessTime;
    @JsonProperty("local_file_id")
    private String localFileId;
}
