package com.service.kyc.ors;

import com.DTO.KYC.ors.response.BusinessActivityData;
import com.models.kyc.ors.BusinessActivityEntity;

public interface BusinessActivityService {
    BusinessActivityEntity createBusinessActivity(BusinessActivityData activityData);
    BusinessActivityEntity findBusinessActivityInfo(Long id);

}
