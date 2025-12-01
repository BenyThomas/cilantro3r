package com.repository.administration;

import com.DTO.administration.*;
import com.config.SYSENV;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helper.DateUtil;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Repository
public class AdministrationRepository {
    @Autowired
    @Qualifier("amgwdb")
    JdbcTemplate jdbcTemplate;

    @Autowired
    ObjectMapper jacksonMapper;

    @Autowired
    SYSENV sysenv;

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AdministrationRepository.class);

    public List<Map<String, Object>> getAdministrationModules(String roleId) {
        List<Map<String, Object>> result = null;
        try {
            result = this.jdbcTemplate.queryForList("select a.name,a.url,a.sub_url,a.url2,a.description from permissions a INNER join module_roles b on a.module_id=b.module_id WHERE b.module_id = 10 and b.role_id = ?", roleId);
        } catch (Exception e) {
            LOGGER.info("ERROR ON GETTING BATCH : {}", e.getMessage());
        }
        return result;
    }

    public String getCostOptimizationsInfo(String branchCode, String branch, String fromDate,
                                  String toDate, String draw, String start, String rowPerPage, String searchValue,
                                  String columnName, String columnSortOrder)   {
        List<Map<String, Object>> results;
        String mainSql;
        int totalRecordWithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        if (fromDate == null || fromDate.length() < 10)
            fromDate = "2024-01-01" + fromDate;
        if (toDate == null || toDate.length() < 10)
            toDate = DateUtil.now("yyyy-MM-dd HH:mm:ss");
        if (Objects.equals(rowPerPage, "-1")) {
            rowPerPage = "10";
        }
        try {
            if (branchCode.equals("060")) {
                if (branch == null || branch.isEmpty()) {
                    mainSql = "SELECT count(c.id) FROM cost_optimization c INNER JOIN cost_optimization_service s ON c.service_id = s.id INNER JOIN branches b ON c.branch_code = b.code where c.created_date>=? and c.created_date<=? ";
                    totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{fromDate, toDate}, Integer.class);
                } else {
                    mainSql = "SELECT count(c.id) FROM cost_optimization c INNER JOIN cost_optimization_service s ON c.service_id = s.id INNER JOIN branches b ON c.branch_code = b.code where c.created_date>=? and c.created_date<=? and branch_code=? ";

                    totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{fromDate, toDate, branch}, Integer.class);
                }
            } else {
                mainSql = "SELECT count(c.id) FROM cost_optimization c INNER JOIN cost_optimization_service s ON c.service_id = s.id INNER JOIN branches b ON c.branch_code = b.code where c.created_date>=? and c.created_date<=? and branch_code=? ";
                totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{fromDate, toDate, branchCode}, Integer.class);
            }
            String searchQuery;
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                if (branchCode.equals("060")) {
                    searchQuery = " WHERE concat(s.name,' ',c.created_by,' ',b.name) LIKE ? ";
                    if (branch == null || branch.isEmpty()) {
                        mainSql = "SELECT c.id, s.service, c.cost, c.created_by, c.created_date, b.name branch FROM cost_optimization c INNER JOIN cost_optimization_service s ON c.service_id = s.id INNER JOIN branches b ON c.branch_code = b.code " + searchQuery +  "and c.created_date>=? and c.created_date<=?" + " ORDER BY " + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        results = jdbcTemplate.queryForList(mainSql, searchValue, fromDate, toDate);
                        totalRecordWithFilter = jdbcTemplate.queryForObject("SELECT count(c.id) FROM cost_optimization c INNER JOIN branches b ON c.branch_code = b.code" + searchQuery + "and c.created_date>=? and c.created_date<=? ", new Object[]{searchValue, fromDate, toDate}, Integer.class);
                    } else {
                        mainSql = "SELECT c.id, s.service, c.cost, c.created_by, c.created_date, b.name branch FROM cost_optimization c INNER JOIN cost_optimization_service s ON c.service_id = s.id INNER JOIN branches b ON c.branch_code = b.code " + searchQuery  + "and c.created_date>=? and c.created_date<=? and c.branch_code=?" + " ORDER BY " + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        results = jdbcTemplate.queryForList(mainSql, searchValue, fromDate, toDate, branch);
                        totalRecordWithFilter = jdbcTemplate.queryForObject("SELECT count(c.id) FROM cost_optimization c INNER JOIN branches b ON c.branch_code = b.code" + searchQuery + "and c.created_date>=? and c.created_date<=? and c.branch_code=?", new Object[]{searchValue, fromDate, toDate, branch}, Integer.class);
                    }
                } else {
                    searchQuery = " WHERE concat(s.name,' ',c.created_by,' ',b.name) LIKE ? and branch_code=? and created_date>=? and created_date<=? ";
                    totalRecordWithFilter = jdbcTemplate.queryForObject("SELECT count(id) FROM cost_optimization c " + searchQuery, new Object[]{searchValue, branchCode, fromDate, toDate}, Integer.class);
                    mainSql = "SELECT c.id, s.service, c.cost, c.created_by, c.created_date, b.name branch FROM cost_optimization c INNER JOIN cost_optimization_service s ON c.service_id = s.id INNER JOIN branches b ON c.branch_code = b.code " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                    results = jdbcTemplate.queryForList(mainSql, searchValue, branchCode, fromDate, toDate);
                }
            } else {
                totalRecordWithFilter = totalRecords;
                if (branchCode.equals("060")) {
                    if (branch == null || branch.isEmpty()) {
                        mainSql = "SELECT c.id, s.service, c.cost, c.created_by, c.created_date, b.name branch FROM cost_optimization c INNER JOIN cost_optimization_service s ON c.service_id = s.id INNER JOIN branches b ON c.branch_code = b.code where c.created_date>=? and c.created_date<=? ORDER BY " + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        totalRecordWithFilter = jdbcTemplate.queryForObject("SELECT count(c.id) FROM cost_optimization c INNER JOIN branches b ON c.branch_code = b.code where c.created_date>=? and c.created_date<=?", new Object[]{fromDate, toDate}, Integer.class);
                        results = jdbcTemplate.queryForList(mainSql, fromDate, toDate);
                    } else {
                        mainSql = "SELECT c.id, s.service, c.cost, c.created_by, c.created_date, b.name branch FROM cost_optimization c INNER JOIN cost_optimization_service s ON c.service_id = s.id INNER JOIN branches b ON c.branch_code = b.code where c.created_date>=? and c.created_date<=? and c.branch_code=? ORDER BY " + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        totalRecordWithFilter = jdbcTemplate.queryForObject("SELECT count(c.id) FROM cost_optimization c INNER JOIN branches b ON c.branch_code = b.code where c.created_date>=? and c.created_date<=? and c.branch_code=?", new Object[]{fromDate, toDate, branch}, Integer.class);
                        results = jdbcTemplate.queryForList(mainSql, fromDate, toDate, branch);
                    }
                } else {
                    mainSql = "SELECT c.id, s.service, c.cost, c.created_by, c.created_date, b.name branch FROM cost_optimization c INNER JOIN cost_optimization_service s ON c.service_id = s.id INNER JOIN branches b ON c.branch_code = b.code where created_date>=? and created_date<=? and branch_code=? ORDER BY " + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                    totalRecordWithFilter = jdbcTemplate.queryForObject("SELECT count(c.id) FROM cost_optimization c where c.created_date>=? and c.created_date<=? and c.branch_code=?", new Object[]{fromDate, toDate, branchCode}, Integer.class);
                    results = jdbcTemplate.queryForList(mainSql, fromDate, toDate, branchCode);
                }
            }
            jsonString = jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            // ex.printStackTrace();
            LOGGER.debug("RequestBody: ", ex);
        }
        return "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordWithFilter + "\",\"aaData\":" + jsonString + "}";
    }

    public String getRiskIndicatorsInfo(String branchCode, String branch, String category, String type, String fromDate,
                                           String toDate, String draw, String start, String rowPerPage, String searchValue,
                                           String columnName, String columnSortOrder) {
        List<Map<String, Object>> results;
        String mainSql;
        int totalRecordWithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        if (fromDate == null || fromDate.length() < 10)
            fromDate = "2024-01-01" + fromDate;
        if (toDate == null || toDate.length() < 10)
            toDate = DateUtil.now("yyyy-MM-dd HH:mm:ss");
        if (Objects.equals(rowPerPage, "-1")) {
            rowPerPage = "10";
        }
        try {
            if (branchCode.equals("060")) {
                if (branch == null || branch.isEmpty()) {
                    mainSql = "SELECT count(c.id) FROM risk_indicator c INNER JOIN risk_indicator_service s ON c.indicator_id = s.id INNER JOIN branches b ON c.branch_code = b.code where c.created_date>=? and c.created_date<=? ";
                    totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{fromDate, toDate}, Integer.class);
                } else {
                    mainSql = "SELECT count(c.id) FROM risk_indicator c INNER JOIN risk_indicator_service s ON c.indicator_id = s.id INNER JOIN branches b ON c.branch_code = b.code where c.created_date>=? and c.created_date<=? and branch_code=? ";

                    totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{fromDate, toDate, branch}, Integer.class);
                }
            } else {
                mainSql = "SELECT count(c.id) FROM risk_indicator c INNER JOIN risk_indicator_service s ON c.indicator_id = s.id INNER JOIN branches b ON c.branch_code = b.code where c.created_date>=? and c.created_date<=? and branch_code=? ";
                totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{fromDate, toDate, branchCode}, Integer.class);
            }
            String searchQuery;
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                if (branchCode.equals("060")) {
                    searchQuery = " WHERE concat(s.name,' ',c.created_by,' ',b.name) LIKE ? ";
                    if (branch == null || branch.isEmpty()) {
                        mainSql = "SELECT c.id, s.indicator, s.description, c.value, c.created_by, c.created_date, b.name branch FROM risk_indicator c INNER JOIN risk_indicator_service s ON c.indicator_id = s.id INNER JOIN branches b ON c.branch_code = b.code " + searchQuery +  "and c.created_date>=? and c.created_date<=?" + " ORDER BY " + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        results = jdbcTemplate.queryForList(mainSql, searchValue, fromDate, toDate);
                        totalRecordWithFilter = jdbcTemplate.queryForObject("SELECT count(c.id) FROM risk_indicator c INNER JOIN branches b ON c.branch_code = b.code" + searchQuery + "and c.created_date>=? and c.created_date<=? ", new Object[]{searchValue, fromDate, toDate}, Integer.class);
                    } else {
                        mainSql = "SELECT c.id, s.indicator, s.description, c.value, c.created_by, c.created_date, b.name branch FROM risk_indicator c INNER JOIN risk_indicator_service s ON c.indicator_id = s.id INNER JOIN branches b ON c.branch_code = b.code " + searchQuery  + "and c.created_date>=? and c.created_date<=? and c.branch_code=?" + " ORDER BY " + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        results = jdbcTemplate.queryForList(mainSql, searchValue, fromDate, toDate, branch);
                        totalRecordWithFilter = jdbcTemplate.queryForObject("SELECT count(c.id) FROM risk_indicator c INNER JOIN branches b ON c.branch_code = b.code" + searchQuery + "and c.created_date>=? and c.created_date<=? and c.branch_code=?", new Object[]{searchValue, fromDate, toDate, branch}, Integer.class);
                    }
                } else {
                    searchQuery = " WHERE concat(s.indicator,' ',c.created_by,' ',b.name) LIKE ? and branch_code=? and created_date>=? and created_date<=? ";
                    totalRecordWithFilter = jdbcTemplate.queryForObject("SELECT count(id) FROM risk_indicator " + searchQuery, new Object[]{searchValue, branchCode, fromDate, toDate}, Integer.class);
                    mainSql = "SELECT c.id, s.indicator, s.description, c.value, c.created_by, c.created_date, b.name branch FROM risk_indicator c INNER JOIN risk_indicator_service s ON c.indicator_id = s.id INNER JOIN branches b ON c.branch_code = b.code " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                    results = jdbcTemplate.queryForList(mainSql, searchValue, branchCode, fromDate, toDate);
                }
            } else {
                totalRecordWithFilter = totalRecords;
                if (branchCode.equals("060")) {
                    if (branch == null || branch.isEmpty()) {
                        mainSql = "SELECT c.id, s.indicator, s.description, c.value, c.created_by, c.created_date, b.name branch FROM risk_indicator c INNER JOIN risk_indicator_service s ON c.indicator_id = s.id INNER JOIN branches b ON c.branch_code = b.code where c.created_date>=? and c.created_date<=? ORDER BY " + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        totalRecordWithFilter = jdbcTemplate.queryForObject("SELECT count(c.id) FROM risk_indicator c INNER JOIN branches b ON c.branch_code = b.code where c.created_date>=? and c.created_date<=?", new Object[]{fromDate, toDate}, Integer.class);
                        results = jdbcTemplate.queryForList(mainSql, fromDate, toDate);
                    } else {
                        mainSql = "SELECT c.id, s.indicator, s.description, c.value, c.created_by, c.created_date, b.name branch FROM risk_indicator c INNER JOIN risk_indicator_service s ON c.indicator_id = s.id INNER JOIN branches b ON c.branch_code = b.code where c.created_date>=? and c.created_date<=? and c.branch_code=? ORDER BY " + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        totalRecordWithFilter = jdbcTemplate.queryForObject("SELECT count(c.id) FROM risk_indicator c INNER JOIN branches b ON c.branch_code = b.code where c.created_date>=? and c.created_date<=? and c.branch_code=?", new Object[]{fromDate, toDate, branch}, Integer.class);
                        results = jdbcTemplate.queryForList(mainSql, fromDate, toDate, branch);
                    }
                } else {
                    mainSql = "SELECT c.id, s.indicator, s.description, c.value, c.created_by, c.created_date, b.name branch FROM risk_indicator c INNER JOIN risk_indicator_service s ON c.indicator_id = s.id INNER JOIN branches b ON c.branch_code = b.code where c.created_date>=? and c.created_date<=? and c.branch_code=? ORDER BY " + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                    totalRecordWithFilter = jdbcTemplate.queryForObject("SELECT count(c.id) FROM risk_indicator c where c.created_date>=? and c.created_date<=? and c.branch_code=?", new Object[]{fromDate, toDate, branchCode}, Integer.class);
                    results = jdbcTemplate.queryForList(mainSql, fromDate, toDate, branchCode);
                }
            }
            jsonString = jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            // ex.printStackTrace();
            LOGGER.debug("RequestBody: ", ex);
        }
        return "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordWithFilter + "\",\"aaData\":" + jsonString + "}";
    }

    public List<Map<String, Object>> branches() {
        try {
            return this.jdbcTemplate.queryForList("SELECT * from branches");
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            return null;
        }
    }

    public List<String> branchNames() {
        try {
            return this.jdbcTemplate.queryForList("SELECT name from branches", String.class);
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            return null;
        }
    }

    public List<Map<String, Object>> parameters() {
        try {
            return this.jdbcTemplate.queryForList("SELECT id, service from cost_optimization_service");
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            return null;
        }
    }

    public List<String> parameterColumns() {
        try {
            return this.jdbcTemplate.queryForList("SELECT service from cost_optimization_service", String.class);
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            return null;
        }
    }

    public List<Map<String, Object>> parameterValues() {
        try {
            return this.jdbcTemplate.queryForList("SELECT b.name branch, service, cost from cost_optimization c " +
                    "join cost_optimization_service cs on c.service_id = cs.id join branches b " +
                    "on c.branch_code = b.code order by branch");
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            return null;
        }
    }

    public List<Map<String, Object>> indicators() {
        try {
            return this.jdbcTemplate.queryForList("SELECT id, indicator from risk_indicator_service");
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            return null;
        }
    }

    public List<String> indicatorColumns() {
        try {
            return this.jdbcTemplate.queryForList("SELECT indicator from risk_indicator_service", String.class);
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            return null;
        }
    }

    public List<Map<String, Object>> indicatorValues() {
        try {
            return this.jdbcTemplate.queryForList("SELECT b.name branch, indicator, description, value from " +
                    "risk_indicator r join risk_indicator_service rs on r.indicator_id = rs.id join branches b on " +
                    "r.branch_code = b.code order by branch");
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            return null;
        }
    }

    public String addCostOptimization(AddCostOptimizationParameter costOptimizationForm) {
        int result = -1;
        try {
            final String INSERT_SQL = "INSERT INTO cost_optimization_service (created_by,created_date,service) values (?,?,?)";
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update((Connection connection) -> {
                PreparedStatement ps = connection.prepareStatement(INSERT_SQL, new String[]{"id"});
                ps.setString(1, costOptimizationForm.createdBy);
                ps.setString(2, costOptimizationForm.createdDate);
                ps.setString(3, costOptimizationForm.parameter);

                return ps;
            }, keyHolder);
            result = Objects.requireNonNull(keyHolder.getKey()).intValue();
        } catch (DataAccessException e) {
            LOGGER.info("addCostOptimization error: {}", e.getMessage());
        }
        if (result != -1) {
            return "{\"responseCode\":\"0\", \"message\":\"Cost optimization parameter added successfully!\"}";
        } else {
            return "{\"responseCode\":\"99\", \"message\":\"Failed to add cost optimization parameter!\"}";
        }
    }

    public String addRiskIndicator(AddRiskIndicatorService riskIndicatorForm) {
        int result = -1;
        try {
            final String INSERT_SQL = "INSERT INTO risk_indicator_service (created_by,created_date,indicator,description) values (?,?,?,?)";
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update((Connection connection) -> {
                PreparedStatement ps = connection.prepareStatement(INSERT_SQL, new String[]{"id"});
                ps.setString(1, riskIndicatorForm.createdBy);
                ps.setString(2, riskIndicatorForm.createdDate);
                ps.setString(3, riskIndicatorForm.service);
                ps.setString(4, riskIndicatorForm.description);

                return ps;
            }, keyHolder);
            result = Objects.requireNonNull(keyHolder.getKey()).intValue();
        } catch (DataAccessException e) {
            LOGGER.info("addRiskIndicator error: {}", e.getMessage());
        }
        if (result != -1) {
            return "{\"responseCode\":\"0\", \"message\":\"Risk indicator service added successfully!\"}";
        } else {
            return "{\"responseCode\":\"99\", \"message\":\"Failed to add risk indicator service!\"}";
        }
    }

    public String branchAddCostOptimization(AddCostOptimizationForm costOptimizationForm) {
        int result = -1;
        try {
            final String INSERT_SQL = "INSERT INTO cost_optimization (branch_code, service_id, cost, created_by, created_date) values (?,?,?,?,?)";
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update((Connection connection) -> {
                PreparedStatement ps = connection.prepareStatement(INSERT_SQL, new String[]{"id"});
                ps.setString(1, costOptimizationForm.getBranchCode());
                ps.setLong(2, costOptimizationForm.getServiceId());
                ps.setBigDecimal(3, costOptimizationForm.getCost());
                ps.setString(4, costOptimizationForm.getCreatedBy());
                ps.setTimestamp(5, new Timestamp(costOptimizationForm.getCreatedDate().getTime()));

                return ps;
            }, keyHolder);
            result = Objects.requireNonNull(keyHolder.getKey()).intValue();
        } catch (DataAccessException e) {
            LOGGER.info("addBranchCostOptimization error: {}", e.getMessage());
        }
        if (result != -1) {
            return "{\"responseCode\":\"0\", \"message\":\"Cost optimization added successfully!\"}";
        } else {
            return "{\"responseCode\":\"99\", \"message\":\"Failed to add cost optimization!\"}";
        }
    }

    public String branchEditCostOptimization(EditCostOptimizationForm costOptimizationForm) {
        int result = -1;
        try {
            final String UPDATE_SQL = "UPDATE cost_optimization SET branch_code = ?, service_id = ?, cost = ?, updated_by = ?, updated_date = ? WHERE id = ?";
            result = jdbcTemplate.update((Connection connection) -> {
                PreparedStatement ps = connection.prepareStatement(UPDATE_SQL);
                ps.setString(1, costOptimizationForm.getBranchCode());
                ps.setLong(2, costOptimizationForm.getServiceId());
                ps.setBigDecimal(3, costOptimizationForm.getCost());
                ps.setString(4, costOptimizationForm.getUpdatedBy());
                ps.setTimestamp(5, new Timestamp(costOptimizationForm.getUpdatedDate().getTime()));
                ps.setLong(6, costOptimizationForm.getId());

                return ps;
            });
        } catch (DataAccessException e) {
            LOGGER.info("editBranchCostOptimization error: {}", e.getMessage());
        }
        if (result != -1) {
            return "{\"responseCode\":\"0\", \"message\":\"Cost optimization edited successfully!\"}";
        } else {
            return "{\"responseCode\":\"99\", \"message\":\"Failed to edit cost optimization!\"}";
        }
    }

    public String branchRemoveCostOptimization(String id) {
        int result = -1;
        try {
            final String DELETE_SQL = "DELETE FROM cost_optimization WHERE id = ?";
            result = jdbcTemplate.update((Connection connection) -> {
                PreparedStatement ps = connection.prepareStatement(DELETE_SQL);
                ps.setLong(1, Long.parseLong(id));

                return ps;
            });
        } catch (DataAccessException e) {
            LOGGER.info("removeBranchCostOptimization error: {}", e.getMessage());
        }
        if (result != -1) {
            return "{\"responseCode\":\"0\", \"message\":\"Cost optimization removed successfully!\"}";
        } else {
            return "{\"responseCode\":\"99\", \"message\":\"Failed to remove cost optimization!\"}";
        }
    }

    public String branchAddRiskIndicator(NewRiskIndicatorForm riskIndicatorForm) {
        int result = -1;
        try {
            final String INSERT_SQL = "INSERT INTO risk_indicator (branch_code, indicator_id, value_type, value, created_by, created_date) values (?,?,?,?,?,?)";
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update((Connection connection) -> {
                PreparedStatement ps = connection.prepareStatement(INSERT_SQL, new String[]{"id"});
                ps.setString(1, riskIndicatorForm.getBranchCode());
                ps.setLong(2, riskIndicatorForm.getIndicatorId());
                ps.setString(3, riskIndicatorForm.getValueType());
                ps.setString(4, riskIndicatorForm.getValue());
                ps.setString(5, riskIndicatorForm.getCreatedBy());
                ps.setTimestamp(6, new Timestamp(riskIndicatorForm.getCreatedDate().getTime()));

                return ps;
            }, keyHolder);
            result = Objects.requireNonNull(keyHolder.getKey()).intValue();
        } catch (DataAccessException e) {
            LOGGER.info("newBranchRiskIndicator error: {}", e.getMessage());
        }
        if (result != -1) {
            return "{\"responseCode\":\"0\", \"message\":\"Risk added successfully!\"}";
        } else {
            return "{\"responseCode\":\"99\", \"message\":\"Failed to add risk!\"}";
        }
    }

    public String branchEditRiskIndicator(EditRiskIndicatorForm riskIndicatorForm) {
        int result = -1;
        try {
            final String UPDATE_SQL = "UPDATE risk_indicator SET branch_code = ?, indicator_id = ?, value_type = ?, value = ?, updated_by = ?, updated_date = ? WHERE id = ?";
            result = jdbcTemplate.update((Connection connection) -> {
                PreparedStatement ps = connection.prepareStatement(UPDATE_SQL);
                ps.setString(1, riskIndicatorForm.getBranchCode());
                ps.setLong(2, riskIndicatorForm.getIndicatorId());
                ps.setString(3, riskIndicatorForm.getValueType());
                ps.setString(4, riskIndicatorForm.getValue());
                ps.setString(5, riskIndicatorForm.getUpdatedBy());
                ps.setTimestamp(6, new Timestamp(riskIndicatorForm.getUpdatedDate().getTime()));
                ps.setLong(7, riskIndicatorForm.getId());

                return ps;
            });
        } catch (DataAccessException e) {
            LOGGER.info("editBranchRiskIndicator error: {}", e.getMessage());
        }
        if (result != -1) {
            return "{\"responseCode\":\"0\", \"message\":\"Risk indicator edited successfully!\"}";
        } else {
            return "{\"responseCode\":\"99\", \"message\":\"Failed to edit risk indicator!\"}";
        }
    }

    public String branchRemoveRiskIndicator(String id) {
        int result = -1;
        try {
            final String DELETE_SQL = "DELETE FROM risk_indicator WHERE id = ?";
            result = jdbcTemplate.update((Connection connection) -> {
                PreparedStatement ps = connection.prepareStatement(DELETE_SQL);
                ps.setLong(1, Long.parseLong(id));

                return ps;
            });
        } catch (DataAccessException e) {
            LOGGER.info("removeBranchRiskIndicator error: {}", e.getMessage());
        }
        if (result != -1) {
            return "{\"responseCode\":\"0\", \"message\":\"Risk indicator removed successfully!\"}";
        } else {
            return "{\"responseCode\":\"99\", \"message\":\"Failed to remove risk indicator!\"}";
        }
    }
}
