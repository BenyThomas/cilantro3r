package com.service.kyc.ors.impl;

import com.DTO.KYC.ors.response.BusinessActivityData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.models.kyc.ors.BusinessActivityEntity;
import com.repository.Kyc.ors.BusinessActivityRepository;
import com.service.kyc.ors.BusinessActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
public class BusinessActivityServiceImpl implements BusinessActivityService {
    private final BusinessActivityRepository activityRepository;
    private final ObjectMapper mapper;
    @Override
    public BusinessActivityEntity createBusinessActivity(BusinessActivityData activityData) {

        return activityRepository.save(
                mapper.convertValue(activityData, BusinessActivityEntity.class)
        );
    }

    @Override
    public BusinessActivityEntity findBusinessActivityInfo(Long id) {
        return activityRepository.findById(id).orElseThrow(()->new RuntimeException("Activity Not found"));
    }
}
