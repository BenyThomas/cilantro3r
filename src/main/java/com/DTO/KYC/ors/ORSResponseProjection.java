package com.DTO.KYC.ors;

import java.util.Set;

public interface ORSResponseProjection {
    boolean getSuccess();
    String getMessage();
    CompanyDataProjection getResponseData(); // Projection for CompanyEntity
    int getResultCode();

    interface CompanyDataProjection {
        String getCertNumber();
        String getLegalName();
        CMDetailedInfoProjection getCMDetailedInfo(); // Projection for CMDetailedInfoEntity
    }

    interface CMDetailedInfoProjection {
        String getCandidateUser();
        String getLastSaveUser();
        Set<DirectorProjection> getDirectors(); // Projection for DirectorEntity
        Set<ShareholderProjection> getShareholders(); // Projection for ShareholderEntity
    }

    interface DirectorProjection {
        String getFirstName();
        String getLastName();
    }

    interface ShareholderProjection {
        String getName();
    }
}
