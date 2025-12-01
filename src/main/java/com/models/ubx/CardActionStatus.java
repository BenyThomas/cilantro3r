package com.models.ubx;

import com.DTO.ubx.CardRegistrationStep;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "card_action_status")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardActionStatus implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private CardRegistrationStep step;

    private String status; // e.g. SUCCESS, FAILED, PENDING
    private String message;
    private LocalDateTime updatedAt;
    @Column(name = "card_id")
    private Long cardId;
    @Column(name = "request_id")
    private Long requestId;
}
