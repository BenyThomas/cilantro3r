package com.repository.itax.GePG;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helper.DateUtil;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class EgaRepository {

    @Autowired
    @Qualifier("partners")
    JdbcTemplate jdbcTemplate;

    @Autowired
    ObjectMapper jacksonMapper;

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(EgaRepository.class);

    public List<Map<String, Object>> getGePGTransactions(String txnStatus, String fromDate, String toDate) {
        List<Map<String, Object>> results = null;

        String mainSql;

        if ((fromDate == null) || (toDate == null) || (txnStatus == null)) {
            fromDate = DateUtil.previosDay(30, "yyyy-MM-dd");
            toDate = DateUtil.now("yyyy-MM-dd");
            txnStatus = "ALL";
        }
        LOGGER.info("Used parameters are STATUS AND FROM-DATE AND TO-DATE: {} {} {}", txnStatus, fromDate, toDate);

        switch (txnStatus) {
            case "ALL":
                //Both completed and pending transactions
                mainSql = "SELECT * FROM  ega_transactions a WHERE a.authorizedon>=? AND a.authorizedon<=? ";
                LOGGER.info("main query for ega transactions {}", mainSql);
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{fromDate, toDate});
                LOGGER.info("query result for ALL ega_transactions: {}", results);
                break;
            case "COMPLETED":
                //status C AND cbs_status C transactions
                mainSql = "SELECT * FROM ega_transactions a WHERE a.authorizedon>=? AND a.authorizedon<=? AND status='C' and cbs_status='C'";
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{fromDate, toDate});
                LOGGER.info("query result for COMPLETED ega_transactions: {}", results);

                break;

            case "PENDING":
                //status P AND cbs_status C transactions
                mainSql = "SELECT * FROM  ega_transactions a WHERE a.status='P' AND a.cbs_status='C' AND a.authorizedon>=? AND a.authorizedon<=? ";
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{fromDate, toDate});
                LOGGER.info("query result for PENDING ega_transactions: {}", results);

                break;
        }

        return results;
    }
}

