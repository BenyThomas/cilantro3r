package com.service.ubx;

import com.models.ubx.CardDetailsEntity;
import com.repository.ubx.VisaCardDetailsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VisaCardDetailsServiceImpl implements VisaCardDetailsService{
    private final VisaCardDetailsRepository visaCardDetailsRepository;
    @Override
    public CardDetailsEntity create(CardDetailsEntity entity) {
        return visaCardDetailsRepository.save(entity);
    }

    @Override
    public CardDetailsEntity update(Long aLong, CardDetailsEntity entity) {
        Optional<CardDetailsEntity> cardDetailsEntity = visaCardDetailsRepository.findById(aLong);
        if(cardDetailsEntity.isPresent()){
            return visaCardDetailsRepository.save(entity);
        }
        return null;
    }

    @Override
    public CardDetailsEntity getById(Long aLong) {
        Optional<CardDetailsEntity> cardDetailsEntity = visaCardDetailsRepository.findById(aLong);
        return cardDetailsEntity.orElse(null);
    }

    @Override
    public void deleteById(Long aLong) {
        visaCardDetailsRepository.deleteById(aLong);
    }

    @Override
    public List<CardDetailsEntity> getAll() {
        return visaCardDetailsRepository.findAll();
    }
}
