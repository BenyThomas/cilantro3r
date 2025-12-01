package com.service.kyc.ors.impl;

import com.models.kyc.ors.ORSResponseEntity;
import com.repository.Kyc.ors.ResponseRepository;
import com.service.kyc.ors.ResponseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
public class ResponseServiceImpl implements ResponseService {
    private final ResponseRepository responseRepository;
    @Override
    public ORSResponseEntity create(ORSResponseEntity response) {
        return responseRepository.save(response);
    }
}
