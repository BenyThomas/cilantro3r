package com.service.kyc.zcsra;

import com.DTO.KYC.zcsra.BiometricRequestPayload;
import com.DTO.KYC.zcsra.DemographicDataRequestPayload;
import com.DTO.KYC.zcsra.Names;
import com.DTO.KYC.zcsra.SignedDemographicDataRequestPayload;
import com.models.kyc.DemographicDataEntity;

import java.io.ByteArrayInputStream;

public interface DemographicDataService {
    DemographicDataEntity getDemographicDataByZanId(DemographicDataRequestPayload payload);
    DemographicDataEntity getDemographicDataByNames(Names names);
    DemographicDataEntity getDemographicDataDirectFromZCSRA(DemographicDataRequestPayload payload) ;
    DemographicDataEntity getDemographicDataFromZCSRAByBiometric(BiometricRequestPayload payload);
    byte[] getDemographicDataInPdfForm(DemographicDataEntity payload);
    SignedDemographicDataRequestPayload getSignedDemographicDataRequestPayload(DemographicDataRequestPayload payload);
    DemographicDataEntity getDemographicDataByZanId(String zanId);
}
