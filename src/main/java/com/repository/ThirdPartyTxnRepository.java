package com.repository;


import com.models.ThirdPartyTxn;
import org.springframework.data.repository.CrudRepository;

public interface ThirdPartyTxnRepository extends CrudRepository<ThirdPartyTxn, Long> {
}

