package com.DTO.ubx;

import com.models.ubx.CardActionStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CardEventDTO {
    private Long id;
    private String step;       // L, A, PR, R, N ...
    private String status;     // BEGIN/SUCCESS/FAILED
    private Integer attemptNo; // if you add this column later; null otherwise
    private String message;
    private LocalDateTime startedAt;  // if present in entity
    private LocalDateTime endedAt;    // if present in entity
    private Long durationMs;          // if present in entity
    private LocalDateTime updatedAt;
    private Long requestId; // nullable

    public static CardEventDTO from(CardActionStatus a) {
        CardEventDTO d = new CardEventDTO();
        d.setId(a.getId());
        d.setStep(a.getStep() != null ? a.getStep().name() : null);
        d.setStatus(a.getStatus());
        d.setMessage(a.getMessage());
        d.setUpdatedAt(a.getUpdatedAt());
        d.setRequestId(a.getRequestId());
        // If you later add structured columns in CardActionStatus, map them here:
        // d.setAttemptNo(a.getAttemptNo());
        // d.setStartedAt(a.getStartedAt());
        // d.setEndedAt(a.getEndedAt());
        // d.setDurationMs(a.getDurationMs());
        return d;
    }

    private static EventTrail toEventDTO(CardActionStatus a) {
        EventTrail d = new EventTrail();
        d.setId(a.getId());
        d.setStep(a.getStep() != null ? a.getStep().name() : null);
        d.setStatus(a.getStatus());
        d.setMessage(a.getMessage());
        d.setUpdatedAt(a.getUpdatedAt());
        // If you later add attemptNo / startedAt / endedAt / durationMs columns, map them here:
        // d.setAttemptNo(a.getAttemptNo());
        // d.setStartedAt(a.getStartedAt());
        // d.setEndedAt(a.getEndedAt());
        // d.setDurationMs(a.getDurationMs());
        d.setRequestId(a.getRequestId());
        return d;
    }
}
