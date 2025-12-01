package com.models.ubx;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import java.util.Date;

@Data
@Entity
@Table(name = "ubx_url_endpoint")
@EntityListeners(AuditingEntityListener.class)
public class UrlEndpointEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "name", nullable = false,unique = true)
    private String name;
    @Column(name = "function", nullable = false)
    private String function;
    @Column(name = "endpoint", nullable = false)
    private String endpoint;
    @Column(name = "created_at", updatable = false,nullable = false)
    @CreatedDate
    private Date created_at;
    @Column(name = "update_at",nullable = false)
    @LastModifiedDate
    private Date update_at;

}

