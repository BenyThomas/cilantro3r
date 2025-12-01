package com.repository.Kyc.ors;

import com.models.kyc.ors.ShareholderSharesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShareholderSharesRepository extends JpaRepository<ShareholderSharesEntity, Long> {
    List<ShareholderSharesEntity> findByCertificate(String certificate);
}
