package com.service.kyc.ors;



import com.DTO.KYC.ors.PayloadDTO;
import com.DTO.KYC.ors.response.ClassifierData;
import com.dao.kyc.response.ors.ClassifierResponseDTO;
import com.models.kyc.ors.ClassifierEntity;

import java.util.List;

public interface ClassifierService {
    ClassifierEntity createClassifier(ClassifierData data);
    ClassifierEntity updateClassifier(ClassifierData data);
    void deleteClassifier(ClassifierData data);
    ClassifierEntity getClassifier(ClassifierData data);
    List<ClassifierEntity> getClassifiers();
    void createClassifiers(List<ClassifierData> data);

    List<ClassifierData> findClassifiersFromDB(PayloadDTO payloadDTO);

    ClassifierResponseDTO<ClassifierData> findClassifiersFromORS(PayloadDTO payloadDTO);
}
