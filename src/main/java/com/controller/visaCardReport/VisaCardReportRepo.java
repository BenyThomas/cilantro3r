package com.controller.visaCardReport;

import com.helper.DateUtil;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class VisaCardReportRepo {

    @Autowired @Qualifier("amgwdb") JdbcTemplate jdbcTemplate;

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(VisaCardReportRepo.class);

    public List<Map<String, Object>> getVisaReportAjax(String branchCode, String statusCode, String fromDate, String toDate) {
        List<Map<String, Object>> results = null;

        String mainSql;

        LOGGER.info("Used parameters are branchCode and statusCode and FROM-DATE AND TO-DATE: {} {} {}", branchCode, statusCode, fromDate, toDate);

        switch (branchCode) {
            case "ALL":
                //Select on all branches
                switch (statusCode){
                    //"CANCELLED" "C" "AP" "CD"
                    case "R":
                        mainSql = "select c.*,b.name as branch_name from card c left join branches b on b.code = c.collecting_branch where c.status=? and c.create_dt>=? and c.create_dt<=?";

                        results = this.jdbcTemplate.queryForList(mainSql, new Object[]{statusCode,fromDate, toDate});
                        break;
                    case "C":
                        //where b.status='C'
                        mainSql = "select c.*,b.name as branch_name from card c left join branches b on b.code = c.collecting_branch where c.status='C' and DATE(c.issued_dt) >= ? AND DATE(c.issued_dt) <= ?";

                        results = this.jdbcTemplate.queryForList(mainSql, new Object[]{fromDate, toDate});
                        break;
                    case "AP":
                        //status='AP' AND DATE(b.approver2_dt) >= ? AND DATE(b.approver2_dt) <= ?
                        mainSql = "select c.*,b.name as branch_name from card c left join branches b on b.code = c.collecting_branch where c.status='AP' and DATE(c.approver2_dt) >= ? AND DATE(c.approver2_dt) <=?";

                        LOGGER.info("main query for visa card report in all branches category {}", mainSql);
                        results = this.jdbcTemplate.queryForList(mainSql, new Object[]{fromDate, toDate});
                        break;
                    case "CD":
                        // DATE(b.dispatched_dt) >= ? AND DATE(b.dispatched_dt) <= ?
                        mainSql = "select c.*,b.name as branch_name from card c left join branches b on b.code = c.collecting_branch where DATE(c.dispatched_dt) >= ? AND DATE(c.dispatched_dt) <=?";

                        results = this.jdbcTemplate.queryForList(mainSql, new Object[]{fromDate, toDate});
                        break;
                   case "RA":
                        mainSql = "select c.*,b.name as branch_name from card c left join branches b on b.code = c.collecting_branch where c.status='RA' AND DATE(c.create_dt) >= ? AND DATE(c.create_dt) <=?";

                        results = this.jdbcTemplate.queryForList(mainSql, new Object[]{fromDate, toDate});
                        break;
                }
                break;

            default:
                switch (statusCode){
                    //"CANCELLED" "C" "AP" "CD"
                    case "R":
                        mainSql = "select c.*,b.name as branch_name from card c left join branches b on b.code = c.collecting_branch where b.code=? and c.status=?";

                        LOGGER.info("main query for visa card report in all branches category {}", mainSql);
                        results = this.jdbcTemplate.queryForList(mainSql, new Object[]{branchCode,statusCode});
                        LOGGER.info("query result for ALL branches and status='R' visa report is: {}", results);
                        break;
                    case "C":
                        //where b.status='C'
                        mainSql = "select c.*,b.name as branch_name from card c left join branches b on b.code = c.collecting_branch where c.status='C' and b.code=? and DATE(c.issued_dt) >= ? AND DATE(c.issued_dt) <= ?";

                        LOGGER.info("main query for visa card report in all branches category {}", mainSql);
                        results = this.jdbcTemplate.queryForList(mainSql, new Object[]{branchCode,fromDate, toDate});
                        LOGGER.info("query result for ALL branches issued card visa report is: {}", results);
                        break;
                    case "AP":
                        //status='AP' AND DATE(b.approver2_dt) >= ? AND DATE(b.approver2_dt) <= ?
                        mainSql = "select c.*,b.name as branch_name from card c left join branches b on b.code = c.collecting_branch where c.status='AP' and b.code=? and DATE(c.approver2_dt) >= ? AND DATE(c.approver2_dt) <=?";

                        LOGGER.info("main query for visa card report in all branches category {}", mainSql);
                        results = this.jdbcTemplate.queryForList(mainSql, new Object[]{branchCode,fromDate, toDate});
                        LOGGER.info("query result for ALL branches Awaiting for printing card visa report is: {}", results);
                        break;
                    case "CD":
                        // DATE(b.dispatched_dt) >= ? AND DATE(b.dispatched_dt) <= ?
                        mainSql = "select c.*,b.name as branch_name from card c left join branches b on b.code = c.collecting_branch where b.code=?  AND DATE(c.dispatched_dt) >= ? AND DATE(c.dispatched_dt) <=?";

                        LOGGER.info("main query for visa card report in all branches category {}", mainSql);
                        results = this.jdbcTemplate.queryForList(mainSql, new Object[]{branchCode,fromDate, toDate});
                        LOGGER.info("query result for ALL branches Dispatched cards visa report is: {}", results);
                        break;

                    case "RA":
                        mainSql = "select c.*,b.name as branch_name from card c left join branches b on b.code = c.collecting_branch where c.status='RA' AND DATE(c.create_dt) >= ? AND DATE(c.create_dt) <=?";

                        results = this.jdbcTemplate.queryForList(mainSql, new Object[]{fromDate, toDate});
                        break;
                }

                break;
        }

        return results;
    }
}
