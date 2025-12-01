package com.service.ubx;

import com.models.ubx.LinkedAccountEntity;
import com.repository.ubx.LinkedAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VisaLinkedAccountServiceImpl implements VisaLinkedAccountService{
    private final LinkedAccountRepository linkedAccountRepository;

    @Override
    public LinkedAccountEntity create(LinkedAccountEntity entity) {
        return linkedAccountRepository.save(entity);
    }

    @Override
    public LinkedAccountEntity update(Long aLong, LinkedAccountEntity entity) {
        Optional<LinkedAccountEntity> linkedAccountEntity = linkedAccountRepository.findById(aLong);
        if(linkedAccountEntity.isPresent()){
            entity.setId(aLong);
            return linkedAccountRepository.save(entity);
        }
        return null;
    }

    @Override
    public LinkedAccountEntity getById(Long aLong) {
        return linkedAccountRepository.findById(aLong).orElse(null);
    }

    @Override
    public void deleteById(Long aLong) {
        linkedAccountRepository.deleteById(aLong);

    }

    @Override
    public List<LinkedAccountEntity> getAll() {
        return linkedAccountRepository.findAll();
    }
}
