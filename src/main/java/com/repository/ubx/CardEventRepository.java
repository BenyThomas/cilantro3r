package com.repository.ubx;

import com.DTO.ubx.CardEvent;
import com.models.ubx.CardEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardEventRepository extends JpaRepository<CardEventEntity, Long> {
    List<CardEventEntity> findByCardIdOrderByStartedAtAscAttemptAsc(Long cardId);
}
