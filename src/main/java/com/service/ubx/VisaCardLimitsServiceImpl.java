package com.service.ubx;

import com.models.ubx.CardLimitsEntity;
import com.repository.ubx.VisaCardLimitsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VisaCardLimitsServiceImpl implements VisaCardLimitsService{
    private final VisaCardLimitsRepository visaCardLimitsRepository;
    @Override
    public CardLimitsEntity create(CardLimitsEntity entity) {
        return visaCardLimitsRepository.save(entity);
    }

    @Override
    public CardLimitsEntity update(Long aLong, CardLimitsEntity entity) {
        Optional<CardLimitsEntity> cardLimitsEntity = visaCardLimitsRepository.findById(aLong);
        if(cardLimitsEntity.isPresent()){
            entity.setId(aLong);
            return visaCardLimitsRepository.save(entity);
        }
        return null;
    }

    @Override
    public CardLimitsEntity getById(Long aLong) {
        return visaCardLimitsRepository.findById(aLong).orElse(null);
    }

    @Override
    public void deleteById(Long aLong) {
        visaCardLimitsRepository.deleteById(aLong);

    }

    @Override
    public List<CardLimitsEntity> getAll() {
        return visaCardLimitsRepository.findAll();
    }
}
