package com.models.ubx;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "card_events")
public class CardEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK -> card.id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id")
    private CardDetailsEntity card;

    @Column(name = "step_code", length = 40, nullable = false)
    private String stepCode; // LINK_CARD, REISSUE_PIN, ISSUE_TO_CBS, CHARGE_CARD, CHANGE_PIN, ACTIVATE_CARD

    @Column(name = "status", length = 20, nullable = false)
    private String status;   // SUCCESS, FAILED, PENDING, RETRYING

    @Column(name = "attempt_no")
    private Integer attempt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "error_message", length = 4000)
    private String errorMessage;

    @Lob
    @Column(name = "meta_json")
    private String metaJson; // optional raw JSON for debugging
}

