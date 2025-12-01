package com.repository.ubx;

import com.DTO.ubx.CardRegistrationStep;
import com.models.ubx.CardActionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CardActionStatusRepository extends JpaRepository<CardActionStatus, Long> {
    Optional<CardActionStatus> findByStepAndStatusAndCardId(CardRegistrationStep step, CardActionStatus status, Long cardId);
    List<CardActionStatus> findByStepAndCardId(CardRegistrationStep step, Long cardId);
    List<CardActionStatus> findByCardId(Long cardId);
    List<CardActionStatus> findByStatus(String status);

    Collection<CardActionStatus> findByCardIdAndStatus(Long cardId, String failed);
    boolean existsByCardIdAndStepAndStatus(Long cardId, CardRegistrationStep step, String success);

    long countByCardIdAndStep(Long id, CardRegistrationStep step);
    List<CardActionStatus> findByCardIdOrderByUpdatedAtDesc(Long cardId);

}
