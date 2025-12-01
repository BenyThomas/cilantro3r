package com.service.ubx;

import com.models.ubx.CardProductLimitsEntity;
import com.repository.ubx.VisaCardProductLimitsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VisaCardProductLimitsServiceImpl implements VisaCardProductLimitsService{
    private final VisaCardProductLimitsRepository visaCardProductLimitsRepository;
    @Override
    public CardProductLimitsEntity create(CardProductLimitsEntity entity) {
        return visaCardProductLimitsRepository.save(entity);
    }

    @Override
    public CardProductLimitsEntity update(Long aLong, CardProductLimitsEntity entity) {
        Optional<CardProductLimitsEntity> cardProductLimitsEntity = visaCardProductLimitsRepository.findById(aLong);
        if(cardProductLimitsEntity.isPresent()){
            entity.setId(aLong);
            return visaCardProductLimitsRepository.save(entity);
        }
        return null;
    }

    @Override
    public CardProductLimitsEntity getById(Long aLong) {
        return visaCardProductLimitsRepository.findById(aLong).orElse(null);
    }

    @Override
    public void deleteById(Long aLong) {
        visaCardProductLimitsRepository.deleteById(aLong);
    }

    @Override
    public List<CardProductLimitsEntity> getAll() {
        return visaCardProductLimitsRepository.findAll();
    }
}
