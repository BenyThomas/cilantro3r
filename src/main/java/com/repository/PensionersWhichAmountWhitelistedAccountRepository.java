package com.repository;

import com.models.PensionersWhichAmountWhitelistedAccount;
import com.models.Transfers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.Optional;

@Transactional
@Repository
public interface PensionersWhichAmountWhitelistedAccountRepository extends JpaRepository<PensionersWhichAmountWhitelistedAccount, Long> {

}
