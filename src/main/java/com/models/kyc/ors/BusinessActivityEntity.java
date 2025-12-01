package com.models.kyc.ors;

import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
import java.util.List;

@Data
@Table(name = "brela_business_activity")
@Entity
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@ToString
public class BusinessActivityEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID")
    private Long id;
    @JsonProperty("name_of_activity_category")
    @Column(name = "name_of_activity_category")
    private String nameOfActivityCategory;
    @JsonProperty("name_of_activity_division")
    @Column(name = "name_of_activity_division")
    private String nameOfActivityDivision;
    @JsonProperty("name_of_activity_group")
    @Column(name = "name_of_activity_group")
    private String nameOfActivityGroup;
    @JsonProperty("name_of_activity_class")
    @Column(name = "name_of_activity_class")
    private String nameOfActivityClass;
    @JsonProperty("main_activity")
    @Column(name = "main_activity")
    private String mainActivity;
    @JsonIgnore
    @Column(name = "certificate",  nullable = false)
    private String certificate;
    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private Date createdAt;
    @LastModifiedDate
    @Column(name = "Updated_At", nullable = false)
    private Date updatedAt;
}
