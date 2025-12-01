package com.service.kyc.ors.impl;

import com.DTO.KYC.ors.response.DirectorData;
import com.models.kyc.ors.DirectorEntity;
import com.service.kyc.ors.DirectorService;

import java.util.List;

public class DirectorServiceImpl implements DirectorService {
    @Override
    public DirectorEntity findDirectorById(Long id) {
        return null;
    }

    @Override
    public DirectorEntity createDirector(DirectorData directorData) {
        return null;
    }

    @Override
    public List<DirectorData> findDirectorByCompanyRegNumberAndType(String reg, String type) {
        return null;
    }
}
