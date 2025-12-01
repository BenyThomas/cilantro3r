package com.service.kyc.ors;



import com.DTO.KYC.ors.response.DirectorData;
import com.models.kyc.ors.DirectorEntity;

import java.util.List;

public interface DirectorService {
    DirectorEntity findDirectorById(Long id);
    DirectorEntity createDirector(DirectorData directorData);
    List<DirectorData> findDirectorByCompanyRegNumberAndType(String reg, String type );
}
