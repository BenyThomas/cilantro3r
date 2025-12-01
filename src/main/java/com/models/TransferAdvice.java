package com.models;

import jakarta.persistence.*;
import lombok.Data;

import jakarta.persistence.Column;
import jakarta.persistence.Lob;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "transfer_advices")
@Data
public class TransferAdvice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate valueDate;

    private String channel;

    @Lob
    private String swiftMessage;

    private String messageType;

    private String senderBank;

    private String receiverBank;

    private String direction;

    @Lob
    private byte[] messageInPdf;

    private String senderReference;

    private String relatedReference;

    private LocalDateTime transDate;

    private String currency;

    private BigDecimal amount;

    private String serviceCode;

    private String cbsStatus;

    @Lob
    private String cbsMessage;

    private String status;

    @Column(length = 220)
    private String botStatus;

    private LocalDateTime botStatusTime;
}

