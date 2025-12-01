package com.DTO.ubx;


import lombok.Data;
import java.time.LocalDateTime;

@Data
public class InstantCardSummary {
    private Long id;
    private String reference;
    private String pan;
    private String accountNo;
    private String customerName;
    private String phone;
    private String email;
    private String custId;
    private String customerRimNo;
    private String shortName;
    private String category;
    private LocalDateTime createdAt;
    private String status;
    private String stage;
    private String originatingBranch;
    private String collectingBranch;
    private String lastError;
}
