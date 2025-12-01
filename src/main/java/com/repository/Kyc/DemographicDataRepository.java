package com.repository.Kyc;

import com.models.kyc.DemographicDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DemographicDataRepository extends JpaRepository<DemographicDataEntity, Long> {
    DemographicDataEntity findByPrsnId(Integer prsnId);
    DemographicDataEntity findByPrsnFirstNameAndPrsnLastName(String prsnFirstName, String prsnLastName);
}
