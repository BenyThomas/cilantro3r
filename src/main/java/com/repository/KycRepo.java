/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.repository;

import com.DTO.KYC.CreateMobileAgent;
import com.config.SYSENV;
import com.entities.*;
import com.entities.MoFP.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.helper.DateUtil;
import com.helper.SignRequest;
import com.models.ExistingCustomer;
import com.service.HttpClientService;
import com.service.JasperService;
import com.service.XMLParserService;
import com.zaxxer.hikari.HikariDataSource;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.util.JRLoader;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Repository;

import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static java.time.temporal.TemporalAdjusters.firstDayOfYear;

@Repository
public class KycRepo {

    @Autowired
    @Qualifier("amgwdb")
    JdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("amgwConnection")
    HikariDataSource cilantroDatasource;

    @Autowired
    @Qualifier("gwKycDbConnection")
    JdbcTemplate jdbcKycTemplate;

    @Autowired
    @Qualifier("gwBrinjalDbConnection")
    JdbcTemplate jdbcBrinjalTemplate;

    @Autowired
    @Qualifier("jdbcCbsLive")
    JdbcTemplate jdbcLiveTemplate;

    @Autowired
    ObjectMapper jacksonMapper;

    @Autowired
    JasperService jasperService;

    @Autowired
    @Qualifier("gwkyc")
    HikariDataSource kycDatasource;

    @Autowired
    SYSENV sysenv;

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(KycRepo.class);

    /*
    GET KYC dashboard
     */
    public List<Map<String, Object>> getKycModules(String roleId) {
        List<Map<String, Object>> result = null;
        try {
            result = this.jdbcTemplate.queryForList("select a.name,a.url,a.order_id from kyc_modules a INNER JOIN kyc_permission_role b on b.kyc_permission_id=a.id where b.role_id=? order by order_id", roleId);
        } catch (Exception e) {
            LOGGER.info("ERROR ON GETTING BATCH : {}", e.getMessage());
        }
        return result;
    }

    /*
    CREATE MOBILE AGENT TO DATABASE
     */
    public int createMobileAgentToKycGw(CreateMobileAgent mobileAgentForm) {
        //save the role and get the last insert ID as role ID
        int userID;
        int result = -1;
        try {
            String password = "password";
            final String INSERT_SQL = "INSERT INTO users (created_by,created_date,account_no_expired,account_no_locked," +
                    "credentials_no_expired,device_id,email,enabled,password,username,branch_code,firstname,middlename," +
                    "lastname,phone,rec_status,first_login,is_agent,account,category) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcKycTemplate.update((Connection connection) -> {
                PreparedStatement ps = connection.prepareStatement(INSERT_SQL, new String[]{"id"});
                ps.setString(1, mobileAgentForm.createdBy);
                ps.setString(2, mobileAgentForm.createdDate);
                ps.setString(3, "1");
                ps.setString(4, "1");
                ps.setString(5, "1");
                ps.setString(6, new BCryptPasswordEncoder().encode(password));
                ps.setString(7, mobileAgentForm.getEmail());
                ps.setString(8, "0");
                ps.setString(9, new BCryptPasswordEncoder().encode(password));
                ps.setString(10, mobileAgentForm.getUsername());
                ps.setString(11, mobileAgentForm.getBranchCode());
                ps.setString(12, mobileAgentForm.getFirstName());
                ps.setString(13, mobileAgentForm.getMiddleName());
                ps.setString(14, mobileAgentForm.getLastName());
                ps.setString(15, mobileAgentForm.getPhoneNumber());
                ps.setString(16, "P");
                ps.setString(17, "Yes");
                ps.setBoolean(18, mobileAgentForm.isAgent());
                ps.setString(19, mobileAgentForm.getAccount());
                ps.setString(20, mobileAgentForm.getCategory());

                return ps;
            }, keyHolder);
            userID = Objects.requireNonNull(keyHolder.getKey()).intValue();
            result = userID;
            //map a user to the role after inserting the user
            final String ROLE_INSERT_SQL = "INSERT INTO role_user (user_id, role_id) values (?, ?)";
            try {
                result = jdbcKycTemplate.update(ROLE_INSERT_SQL, userID, mobileAgentForm.isAgent() ? 2 : 3);
            } catch (Exception e) {
                LOGGER.info("ERROR ON INSERTING ROLE_USER: {}", e.getMessage());
            }
        } catch (DataAccessException e) {
            LOGGER.info("User_M:saveUser: {}", e.getMessage());
        }
        return result;
    }

    public List<Map<String, Object>> branches() {
        try {
            return this.jdbcTemplate.queryForList("SELECT * from branches");
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            return null;
        }
    }

    public String getCustomerInfo(String branchCode, String branch, String category, String type, String fromDate,
                                  String toDate, String draw, String start, String rowPerPage, String searchValue,
                                  String columnName, String columnSortOrder) {
        List<Map<String, Object>> results;
        String mainSql;
        int totalRecordWithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        if (fromDate == null || fromDate.length() < 10)
            fromDate = "2021-10-01" + fromDate;
        if (toDate == null || toDate.length() < 10)
            toDate = DateUtil.now("yyyy-MM-dd HH:mm:ss");
        if (category == null || category.isEmpty())
            category = "NIDA";
        if (type == null || type.isEmpty())
            type = "";
        if (Objects.equals(rowPerPage, "-1")) {
            rowPerPage = "10";
        }
        try {
            if (branchCode.equals("060")) {
                if (branch == null || branch.isEmpty()) {
                    mainSql = "SELECT count(c.id) FROM customers c INNER JOIN branches b ON c.branch_code = b.code where c.created_date>=? and c.created_date<=? and action=? ";
                    if (type.equals("ASSISTED")) {
                        mainSql += "and is_from_mobile=0 ";
                    } else if (type.equals("MOBILE")) {
                        mainSql += "is_from_mobile=1 ";
                    }
                    totalRecords = jdbcKycTemplate.queryForObject(mainSql, new Object[]{fromDate, toDate, category}, Integer.class);
                } else {
                    mainSql = "SELECT count(c.id) FROM customers c INNER JOIN branches b ON c.branch_code = b.code where c.created_date>=? and c.created_date<=? and action=? and branch_code=? ";
                    if (type.equals("ASSISTED")) {
                        mainSql += "and is_from_mobile=0 ";
                    } else if (type.equals("MOBILE")) {
                        mainSql += "and is_from_mobile=1 ";
                    }
                    totalRecords = jdbcKycTemplate.queryForObject(mainSql, new Object[]{fromDate, toDate, category, branch}, Integer.class);
                }
            } else {
                mainSql = "SELECT count(c.id) FROM customers c INNER JOIN branches b ON c.branch_code = b.code where c.created_date>=? and c.created_date<=? and branch_code=? and action=? ";
                if (type.equals("ASSISTED")) {
                    mainSql += "and is_from_mobile=0 ";
                } else if (type.equals("MOBILE")) {
                    mainSql += "and is_from_mobile=1 ";
                }
                totalRecords = jdbcKycTemplate.queryForObject(mainSql, new Object[]{fromDate, toDate, branchCode, category}, Integer.class);
            }
            String searchQuery;
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                if (branchCode.equals("060")) {
                    searchQuery = " WHERE concat(c.last_modified_by,' ',c.rec_status,' ',c.birth_district,' ',c.birth_region,' ',c.cbs_response_code,' ',c.cbs_response_message,' ',c.city,' ',c.dob,' ',c.email_address,' ',c.employer,' ',c.employment_status,' ',c.first_name,' ',c.gender,' ',c.id_number,' ',c.id_type,' ',c.income_source,' ',c.last_name,' ',c.marital_status,' ',c.middle_name,' ',c.nationality,' ',c.phone_number,' ',c.postal_address,' ',c.title,' ',c.branch_code,' ',c.action,' ',b.name) LIKE ? ";
                    if (type.equals("ASSISTED")) {
                        searchQuery += "and is_from_mobile=0";
                    } else if (type.equals("MOBILE")) {
                        searchQuery += "and is_from_mobile=1";
                    }
                    if (branch == null || branch.isEmpty()) {
                        mainSql = "SELECT c.*, b.name branch FROM customers c INNER JOIN branches b ON c.branch_code = b.code " + searchQuery +  "and c.action=? and c.created_date>=? and c.created_date<=?" + " ORDER BY " + columnName + " " + columnSortOrder + " OFFSET " + start + "ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        if (type.equals("ASSISTED")) {
                            mainSql = "SELECT c.*, b.name branch FROM customers c INNER JOIN branches b ON c.branch_code = b.code " + searchQuery +  "and c.action=? and c.created_date>=? and c.created_date<=? and is_from_mobile=0" + " ORDER BY " + columnName + " " + columnSortOrder + " OFFSET " + start + "ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        } else if (type.equals("MOBILE")) {
                            mainSql = "SELECT c.*, b.name branch FROM customers c INNER JOIN branches b ON c.branch_code = b.code " + searchQuery +  "and c.action=? and c.created_date>=? and c.created_date<=? and is_from_mobile=1" + " ORDER BY " + columnName + " " + columnSortOrder + " OFFSET " + start + "ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        }
                        results = this.jdbcKycTemplate.queryForList(mainSql, new Object[]{searchValue, category, fromDate, toDate});
                        totalRecordWithFilter = jdbcKycTemplate.queryForObject("SELECT count(c.id) FROM customers c INNER JOIN branches b ON c.branch_code = b.code" + searchQuery + "and c.action=? and c.created_date>=? and c.created_date<=? ", new Object[]{searchValue, category, fromDate, toDate}, Integer.class);
                    } else {
                        mainSql = "SELECT c.*, b.name branch FROM customers c INNER JOIN branches b ON c.branch_code = b.code " + searchQuery  + "and c.action=? and c.created_date>=? and c.created_date<=? and c.branch_code=?" + " ORDER BY " + columnName + " " + columnSortOrder + " OFFSET " + start + "ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        if (type.equals("ASSISTED")) {
                            mainSql = "SELECT c.*, b.name branch FROM customers c INNER JOIN branches b ON c.branch_code = b.code " + searchQuery  + "and c.action=? and c.created_date>=? and c.created_date<=? and c.branch_code=? and is_from_mobile=0" + " ORDER BY " + columnName + " " + columnSortOrder + " OFFSET " + start + "ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        } else if (type.equals("MOBILE")) {
                            mainSql = "SELECT c.*, b.name branch FROM customers c INNER JOIN branches b ON c.branch_code = b.code " + searchQuery  + "and c.action=? and c.created_date>=? and c.created_date<=? and c.branch_code=? and is_from_mobile=1" + " ORDER BY " + columnName + " " + columnSortOrder + " OFFSET " + start + "ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        }
                        results = this.jdbcKycTemplate.queryForList(mainSql, new Object[]{searchValue, category, fromDate, toDate, branch});
                        totalRecordWithFilter = jdbcKycTemplate.queryForObject("SELECT count(c.id) FROM customers c INNER JOIN branches b ON c.branch_code = b.code" + searchQuery + "and c.action=? and c.created_date>=? and c.created_date<=? and c.branch_code=?", new Object[]{searchValue, category, fromDate, toDate, branch}, Integer.class);
                    }
                } else {
                    searchQuery = " WHERE concat(c.last_modified_by,' ',c.rec_status,' ',c.birth_district,' ',c.birth_region,' ',c.cbs_response_code,' ',c.cbs_response_message,' ',c.city,' ',c.dob,' ',c.email_address,' ',c.employer,' ',c.employment_status,' ',c.first_name,' ',c.gender,' ',c.id_number,' ',c.id_type,' ',c.income_source,' ',c.last_name,' ',c.marital_status,' ',c.middle_name,' ',c.nationality,' ',c.phone_number,' ',c.postal_address,' ',c.title,' ',c.branch_code,' ',c.action,' ',b.name) LIKE ? and branch_code=? and action=? and created_date>=? and created_date<=? ";
                    if (type.equals("ASSISTED")) {
                        searchQuery += "and is_from_mobile=0";
                    } else if (type.equals("MOBILE")) {
                        searchQuery += "and is_from_mobile=1";
                    }
                    totalRecordWithFilter = jdbcKycTemplate.queryForObject("SELECT count(id) FROM customers c " + searchQuery, new Object[]{searchValue, branchCode, category, fromDate, toDate}, Integer.class);
                    mainSql = "SELECT * FROM customers " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " OFFSET " + start + "ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                    results = this.jdbcKycTemplate.queryForList(mainSql, new Object[]{searchValue, branchCode, category, fromDate, toDate});
                }
            } else {
                totalRecordWithFilter = totalRecords;
                if (branchCode.equals("060")) {
                    if (branch == null || branch.isEmpty()) {
                        mainSql = "SELECT c.*, b.name branch FROM customers c INNER JOIN branches b ON c.branch_code = b.code where c.created_date>=? and c.created_date<=? and c.action=? ORDER BY " + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        totalRecordWithFilter = jdbcKycTemplate.queryForObject("SELECT count(c.id) FROM customers c INNER JOIN branches b ON c.branch_code = b.code where c.created_date>=? and c.created_date<=? and c.action=?", new Object[]{fromDate, toDate, category}, Integer.class);
                        if (type.equals("ASSISTED")) {
                            mainSql = "SELECT c.*, b.name branch FROM customers c INNER JOIN branches b ON c.branch_code = b.code where c.created_date>=? and c.created_date<=? and c.action=? and is_from_mobile=0 ORDER BY " + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                            totalRecordWithFilter = jdbcKycTemplate.queryForObject("SELECT count(c.id) FROM customers c INNER JOIN branches b ON c.branch_code = b.code where c.created_date>=? and c.created_date<=? and c.action=? and is_from_mobile=0", new Object[]{fromDate, toDate, category}, Integer.class);
                        } else if (type.equals("MOBILE")) {
                            mainSql = "SELECT c.*, b.name branch FROM customers c INNER JOIN branches b ON c.branch_code = b.code where c.created_date>=? and c.created_date<=? and c.action=? and is_from_mobile=1 ORDER BY " + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                            totalRecordWithFilter = jdbcKycTemplate.queryForObject("SELECT count(c.id) FROM customers c INNER JOIN branches b ON c.branch_code = b.code where c.created_date>=? and c.created_date<=? and c.action=? and is_from_mobile=1", new Object[]{fromDate, toDate, category}, Integer.class);
                        }
                        results = this.jdbcKycTemplate.queryForList(mainSql, new Object[]{fromDate, toDate, category});
                    } else {
                        mainSql = "SELECT c.*, b.name branch FROM customers c INNER JOIN branches b ON c.branch_code = b.code where c.created_date>=? and c.created_date<=? and c.action=? and c.branch_code=? ORDER BY " + columnName + " " + columnSortOrder + " OFFSET " + start + "ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        totalRecordWithFilter = jdbcKycTemplate.queryForObject("SELECT count(c.id) FROM customers c INNER JOIN branches b ON c.branch_code = b.code where c.created_date>=? and c.created_date<=? and c.action=? and c.branch_code=?", new Object[]{fromDate, toDate, category, branch}, Integer.class);
                        if (type.equals("ASSISTED")) {
                            mainSql = "SELECT c.*, b.name branch FROM customers c INNER JOIN branches b ON c.branch_code = b.code where c.created_date>=? and c.created_date<=? and c.action=? and c.branch_code=? and is_from_mobile=0 ORDER BY " + columnName + " " + columnSortOrder + " OFFSET " + start + "ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                            totalRecordWithFilter = jdbcKycTemplate.queryForObject("SELECT count(c.id) FROM customers c INNER JOIN branches b ON c.branch_code = b.code where c.created_date>=? and c.created_date<=? and c.action=? and c.branch_code=? and is_from_mobile=0", new Object[]{fromDate, toDate, category, branch}, Integer.class);
                        } else if (type.equals("MOBILE")) {
                            mainSql = "SELECT c.*, b.name branch FROM customers c INNER JOIN branches b ON c.branch_code = b.code where c.created_date>=? and c.created_date<=? and c.action=? and c.branch_code=? and is_from_mobile=1 ORDER BY " + columnName + " " + columnSortOrder + " OFFSET " + start + "ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                            totalRecordWithFilter = jdbcKycTemplate.queryForObject("SELECT count(c.id) FROM customers c INNER JOIN branches b ON c.branch_code = b.code where c.created_date>=? and c.created_date<=? and c.action=? and c.branch_code=? and is_from_mobile=1", new Object[]{fromDate, toDate, category, branch}, Integer.class);
                        }
                        results = this.jdbcKycTemplate.queryForList(mainSql, new Object[]{fromDate, toDate, category, branch});
                    }
                } else {
                    mainSql = "SELECT * FROM customers where created_date>=? and created_date<=? and branch_code=? and action=? ORDER BY " + columnName + " " + columnSortOrder + " OFFSET " + start + "ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                    totalRecordWithFilter = jdbcKycTemplate.queryForObject("SELECT count(c.id) FROM customers c where c.created_date>=? and c.created_date<=? and c.branch_code=? and c.action=?", new Object[]{fromDate, toDate, branchCode, category}, Integer.class);
                    if (type.equals("ASSISTED")) {
                        mainSql = "SELECT * FROM customers where created_date>=? and created_date<=? and branch_code=? and action=? and is_from_mobile=0 ORDER BY " + columnName + " " + columnSortOrder + " OFFSET " + start + "ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        totalRecordWithFilter = jdbcKycTemplate.queryForObject("SELECT count(c.id) FROM customers c where c.created_date>=? and c.created_date<=? and c.branch_code=? and c.action=? and is_from_mobile=0", new Object[]{fromDate, toDate, branchCode, category}, Integer.class);
                    } else if (type.equals("MOBILE")) {
                        mainSql = "SELECT * FROM customers where created_date>=? and created_date<=? and branch_code=? and action=? and is_from_mobile=1 ORDER BY " + columnName + " " + columnSortOrder + " OFFSET " + start + "ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        totalRecordWithFilter = jdbcKycTemplate.queryForObject("SELECT count(c.id) FROM customers c where c.created_date>=? and c.created_date<=? and c.branch_code=? and c.action=? and is_from_mobile=1", new Object[]{fromDate, toDate, branchCode, category}, Integer.class);
                    }
                    results = this.jdbcKycTemplate.queryForList(mainSql, new Object[]{fromDate, toDate, branchCode, category});
                }
            }
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
           // ex.printStackTrace();
            LOGGER.debug("RequestBody: ", ex);
        }
        return "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordWithFilter + "\",\"aaData\":" + jsonString + "}";
    }

    public String getAccountsInfo(String branchCode, String branch, String category, String agent,  String fromDate,
                                  String toDate, String hasMobileChannel, String draw, String start, String rowPerPage,
                                  String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results;
        String mainSql;
        String countSql;
        int totalRecordWithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        if (fromDate == null || fromDate.length() < 10)
            fromDate = "2021-10-01" + fromDate;
        if (toDate == null || toDate.length() < 10)
            toDate = DateUtil.now("yyyy-MM-dd HH:mm:ss");
        if (category == null)
            category = "";
        if (agent == null)
            agent = "";
        try {
            countSql = "SELECT count(a.id) FROM accounts a left join customers c on c.cust_no = a.customer_number where " +
                    "a.created_date>=? and a.created_date<=? and (?='' OR a.connect_to_mobile_channel=?) and " +
                    "(?='' OR a.branch_code=?) and (?='' OR a.created_by=?)";
            if (branchCode.equals("060")) {
                if (branch == null) {
                    branch = "";
                }
                if (!category.isEmpty()) {
                    countSql += "and (?='' OR c.action=?) ";
                    totalRecords = jdbcKycTemplate.queryForObject(countSql, new Object[]{fromDate, toDate, branch, branch, hasMobileChannel, hasMobileChannel, agent, agent, category, category}, Integer.class);
                } else {
                    totalRecords = jdbcKycTemplate.queryForObject(countSql, new Object[]{fromDate, toDate, branch, branch, hasMobileChannel, hasMobileChannel, agent, agent}, Integer.class);
                }
            } else {
                if (!category.isEmpty()) {
                    countSql += "and c.action=? ";
                    totalRecords = jdbcKycTemplate.queryForObject(countSql, new Object[]{fromDate, toDate, branchCode, branchCode, hasMobileChannel, hasMobileChannel, agent, agent, category}, Integer.class);
                } else {
                    LOGGER.info(countSql.replace("?", "'{}'"), fromDate, toDate, branchCode, branchCode, hasMobileChannel, hasMobileChannel, agent, agent);
                    totalRecords = jdbcKycTemplate.queryForObject(countSql, new Object[]{fromDate, toDate, branchCode, branchCode, hasMobileChannel, hasMobileChannel, agent, agent}, Integer.class);
                }
            }
            String searchQuery;
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                countSql += " and concat(a.created_by,' ',a.account_no,' ',a.account_title,' ',a.customer_number,' ',a.product_id,' ',a.phone_number) LIKE ? ";
                searchQuery = " WHERE concat(a.created_by,' ',a.account_no,' ',a.account_title,' ',a.customer_number,' ',a.product_id,' ',a.phone_number) LIKE ? ";
                if (branchCode.equals("060")) {
                    if (!category.isEmpty()) {
                        mainSql = "SELECT a.*, b.name branch, c.* FROM accounts a LEFT JOIN customers c on c.cust_no = a.customer_number inner join branches b on a.branch_code = b.code " + searchQuery + "and c.action=? and a.created_date>=? and a.created_date<=? and (?='' OR a.connect_to_mobile_channel=?) and (?='' OR a.branch_code=?) and (?='' OR a.created_by=?) ORDER BY a." + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        totalRecordWithFilter = jdbcKycTemplate.queryForObject(countSql, new Object[]{fromDate, toDate, hasMobileChannel, hasMobileChannel, branch, branch, agent, agent, category, category, searchValue}, Integer.class);
                        results = this.jdbcKycTemplate.queryForList(mainSql, new Object[]{searchValue, category, fromDate, toDate, hasMobileChannel, hasMobileChannel, branch, branch, agent, agent});
                    } else {
                        mainSql = "SELECT a.*, b.name branch, c.* FROM accounts a LEFT JOIN customers c on c.cust_no = a.customer_number inner join branches b on a.branch_code = b.code " + searchQuery + "and a.created_date>=? and a.created_date<=? and (?='' OR a.connect_to_mobile_channel=?) and (?='' OR a.branch_code=?) and (?='' OR a.created_by=?) ORDER BY a." + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        totalRecordWithFilter = jdbcKycTemplate.queryForObject(countSql, new Object[]{fromDate, toDate, hasMobileChannel, hasMobileChannel, branch, branch, agent, agent, searchValue}, Integer.class);
                        results = this.jdbcKycTemplate.queryForList(mainSql, new Object[]{searchValue, fromDate, toDate, hasMobileChannel, hasMobileChannel, branch, branch, agent, agent});
                    }
                } else {
                    if (!category.isEmpty()) {
                        mainSql = "SELECT a.*, b.name branch, c.* FROM accounts a LEFT JOIN customers c on c.cust_no = a.customer_number inner join branches b on a.branch_code = b.code " + searchQuery + "and c.action=? and a.created_date>=? and a.created_date<=? and (?='' OR a.connect_to_mobile_channel=?) and (?='' OR a.branch_code=?) and (?='' OR a.created_by=?) ORDER BY a." + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        totalRecordWithFilter = jdbcKycTemplate.queryForObject(countSql, new Object[]{fromDate, toDate, hasMobileChannel, hasMobileChannel, branchCode, branchCode, agent, agent, category, category, searchValue}, Integer.class);
                        results = this.jdbcKycTemplate.queryForList(mainSql, new Object[]{searchValue, category, fromDate, toDate, hasMobileChannel, hasMobileChannel, branchCode, branchCode, agent, agent});
                    } else {
                        mainSql = "SELECT a.*, b.name branch, c.* FROM accounts a LEFT JOIN customers c on c.cust_no = a.customer_number inner join branches b on a.branch_code = b.code " + searchQuery + "and a.created_date>=? and a.created_date<=? and (?='' OR a.connect_to_mobile_channel=?) and (?='' OR a.branch_code=?) and (?='' OR a.created_by=?) ORDER BY a." + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        totalRecordWithFilter = jdbcKycTemplate.queryForObject(countSql, new Object[]{fromDate, toDate, hasMobileChannel, hasMobileChannel, branchCode, branchCode, agent, agent, searchValue}, Integer.class);
                        results = this.jdbcKycTemplate.queryForList(mainSql, new Object[]{searchValue, fromDate, toDate, hasMobileChannel, hasMobileChannel, branchCode, branchCode, agent, agent});
                    }
                }
            } else {
                totalRecordWithFilter = totalRecords;
                if (branchCode.equals("060")) {
                    if (!category.isEmpty()) {
                        mainSql = "SELECT a.*, b.name branch, c.* FROM accounts a LEFT JOIN customers c on c.cust_no = a.customer_number inner join branches b on a.branch_code = b.code WHERE c.action=? and a.created_date>=? and a.created_date<=? and (?='' OR a.connect_to_mobile_channel=?) and (?='' OR a.branch_code=?) and (?='' OR a.created_by=?) ORDER BY a." + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        results = this.jdbcKycTemplate.queryForList(mainSql, new Object[]{category, fromDate, toDate, hasMobileChannel, hasMobileChannel, branch, branch, agent, agent});
                    } else {
                        mainSql = "SELECT a.*, b.name branch, c.* FROM accounts a LEFT JOIN customers c on c.cust_no = a.customer_number inner join branches b on a.branch_code = b.code WHERE a.created_date>=? and a.created_date<=? and (?='' OR a.connect_to_mobile_channel=?) and (?='' OR a.branch_code=?) and (?='' OR a.created_by=?) ORDER BY a." + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        results = this.jdbcKycTemplate.queryForList(mainSql, new Object[]{fromDate, toDate, hasMobileChannel, hasMobileChannel, branch, branch, agent, agent});
                    }
                } else {
                    if (!category.isEmpty()) {
                        mainSql = "SELECT a.*, b.name branch, c.* FROM accounts a LEFT JOIN customers c on c.cust_no = a.customer_number inner join branches b on a.branch_code = b.code WHERE c.action=? and a.created_date>=? and a.created_date<=? and (?='' OR a.connect_to_mobile_channel=?) and (?='' OR a.branch_code=?) and (?='' OR a.created_by=?) ORDER BY a." + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        results = this.jdbcKycTemplate.queryForList(mainSql, new Object[]{category, fromDate, toDate, hasMobileChannel, hasMobileChannel, branchCode, branchCode, agent, agent});
                    } else {
                        mainSql = "SELECT a.*, b.name branch, c.* FROM accounts a LEFT JOIN customers c on c.cust_no = a.customer_number inner join branches b on a.branch_code = b.code WHERE a.created_date>=? and a.created_date<=? and (?='' OR a.connect_to_mobile_channel=?) and (?='' OR a.branch_code=?) and (?='' OR a.created_by=?) ORDER BY a." + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        results = this.jdbcKycTemplate.queryForList(mainSql, new Object[]{fromDate, toDate, hasMobileChannel, hasMobileChannel, branchCode, branchCode, agent, agent});
                    }
                }
            }
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        return "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordWithFilter + "\",\"aaData\":" + jsonString + "}";
    }

    public String getAccountsInfo(String branchCode, String branch, String category, String type, String agent, String fromDate,
                                  String toDate, String hasMobileChannel, String draw, String start, String rowPerPage,
                                  String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results;
        String mainSql;
        String countSql;
        int totalRecordWithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        if (fromDate == null || fromDate.length() < 10)
            fromDate = "2021-10-01" + fromDate;
        if (toDate == null || toDate.length() < 10)
            toDate = DateUtil.now("yyyy-MM-dd HH:mm:ss");
        if (category == null) {
            category = "";
        }
        if (type == null) {
            type = "";
        }
        if (agent == null) {
            agent = "";
        }
        if (Objects.equals(columnName, "branch")) {
            columnName = "id";
        }
        if (Objects.equals(rowPerPage, "-1")) {
            rowPerPage = "10";
        }
        try {
            countSql = "SELECT count(a.id) FROM accounts a left join customers c on c.cust_no = a.customer_number where " +
                    "a.created_date>=? and a.created_date<=? and (?='' OR a.branch_code=?) and (?='' OR a.connect_to_mobile_channel=?) " +
                    "and (?='' OR a.created_by=?) ";
            if (type.equals("ASSISTED")) {
                countSql += "and is_from_mobile=0 ";
            } else if (type.equals("MOBILE")) {
                countSql += "and is_from_mobile=1 ";
            }
            if (branchCode.equals("060")) {
                if (branch == null) {
                    branch = "";
                }
                if (!category.isEmpty()) {
                    countSql += "and (?='' OR c.action=?) ";
                    totalRecords = jdbcKycTemplate.queryForObject(countSql, new Object[]{fromDate, toDate, branch, branch, hasMobileChannel, hasMobileChannel, agent, agent, category, category}, Integer.class);
                } else {
                    totalRecords = jdbcKycTemplate.queryForObject(countSql, new Object[]{fromDate, toDate, branch, branch, hasMobileChannel, hasMobileChannel, agent, agent}, Integer.class);
                }
            } else {
                if (!category.isEmpty()) {
                    countSql += "and c.action=? ";
                    totalRecords = jdbcKycTemplate.queryForObject(countSql, new Object[]{fromDate, toDate, branchCode, branchCode, hasMobileChannel, hasMobileChannel, agent, agent, category}, Integer.class);
                } else {
                    LOGGER.info(countSql.replace("?", "'{}'"), fromDate, toDate, branchCode, branchCode, hasMobileChannel, hasMobileChannel, agent, agent);
                    totalRecords = jdbcKycTemplate.queryForObject(countSql, new Object[]{fromDate, toDate, branchCode, branchCode, hasMobileChannel, hasMobileChannel, agent, agent}, Integer.class);
                }
            }
            String searchQuery;
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                countSql += " and concat(a.created_by,' ',a.account_no,' ',a.account_title,' ',a.customer_number,' ',a.product_id,' ',a.phone_number) LIKE ? ";
                searchQuery = " WHERE concat(a.created_by,' ',a.account_no,' ',a.account_title,' ',a.customer_number,' ',a.product_id,' ',a.phone_number) LIKE ? ";
                if (branchCode.equals("060")) {
                    if (!category.isEmpty()) {
                        mainSql = "SELECT a.*, b.name branch, c.* FROM accounts a LEFT JOIN customers c on c.cust_no = a.customer_number inner join branches b on a.branch_code = b.code " + searchQuery + "and c.action=? and a.created_date>=? and a.created_date<=? and (?='' OR a.connect_to_mobile_channel=?) and (?='' OR a.branch_code=?) and (?='' OR a.created_by=?) ORDER BY a." + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        if (type.equals("ASSISTED")) {
                            mainSql = "SELECT a.*, b.name branch, c.* FROM accounts a LEFT JOIN customers c on c.cust_no = a.customer_number inner join branches b on a.branch_code = b.code " + searchQuery + "and c.action=? and a.created_date>=? and a.created_date<=? and (?='' OR a.connect_to_mobile_channel=?) and (?='' OR a.branch_code=?) and (?='' OR a.created_by=?) and is_from_mobile=0 ORDER BY a." + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        } else if (type.equals("MOBILE")) {
                            mainSql = "SELECT a.*, b.name branch, c.* FROM accounts a LEFT JOIN customers c on c.cust_no = a.customer_number inner join branches b on a.branch_code = b.code " + searchQuery + "and c.action=? and a.created_date>=? and a.created_date<=? and (?='' OR a.connect_to_mobile_channel=?) and (?='' OR a.branch_code=?) and (?='' OR a.created_by=?) and is_from_mobile=1 ORDER BY a." + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        }
                        totalRecordWithFilter = jdbcKycTemplate.queryForObject(countSql, new Object[]{fromDate, toDate, branch, branch, hasMobileChannel, hasMobileChannel, agent, agent, category, category, searchValue}, Integer.class);
                        results = this.jdbcKycTemplate.queryForList(mainSql, new Object[]{searchValue, category, fromDate, toDate, hasMobileChannel, hasMobileChannel, branch, branch, agent, agent});
                    } else {
                        mainSql = "SELECT a.*, b.name branch, c.* FROM accounts a LEFT JOIN customers c on c.cust_no = a.customer_number inner join branches b on a.branch_code = b.code " + searchQuery + "and a.created_date>=? and a.created_date<=? and (?='' OR a.connect_to_mobile_channel=?) and (?='' OR a.branch_code=?) and (?='' OR a.created_by=?) ORDER BY a." + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        if (type.equals("ASSISTED")) {
                            mainSql = "SELECT a.*, b.name branch, c.* FROM accounts a LEFT JOIN customers c on c.cust_no = a.customer_number inner join branches b on a.branch_code = b.code " + searchQuery + "and a.created_date>=? and a.created_date<=? and (?='' OR a.connect_to_mobile_channel=?) and (?='' OR a.branch_code=?) and (?='' OR a.created_by=?) and is_from_mobile=0 ORDER BY a." + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        } else if (type.equals("MOBILE")) {
                            mainSql = "SELECT a.*, b.name branch, c.* FROM accounts a LEFT JOIN customers c on c.cust_no = a.customer_number inner join branches b on a.branch_code = b.code " + searchQuery + "and a.created_date>=? and a.created_date<=? and (?='' OR a.connect_to_mobile_channel=?) and (?='' OR a.branch_code=?) and (?='' OR a.created_by=?) and is_from_mobile=1 ORDER BY a." + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        }
                        totalRecordWithFilter = jdbcKycTemplate.queryForObject(countSql, new Object[]{fromDate, toDate, branch, branch, hasMobileChannel, hasMobileChannel, agent, agent, searchValue}, Integer.class);
                        results = this.jdbcKycTemplate.queryForList(mainSql, new Object[]{searchValue, fromDate, toDate, hasMobileChannel, hasMobileChannel, branch, branch, agent, agent});
                    }
                } else {
                    if (!category.isEmpty()) {
                        mainSql = "SELECT a.*, b.name branch, c.* FROM accounts a LEFT JOIN customers c on c.cust_no = a.customer_number inner join branches b on a.branch_code = b.code " + searchQuery + "and c.action=? and a.created_date>=? and a.created_date<=? and (?='' OR a.connect_to_mobile_channel=?) and (?='' OR a.branch_code=?) and (?='' OR a.created_by=?) ORDER BY a." + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        if (type.equals("ASSISTED")) {
                            mainSql = "SELECT a.*, b.name branch, c.* FROM accounts a LEFT JOIN customers c on c.cust_no = a.customer_number inner join branches b on a.branch_code = b.code " + searchQuery + "and c.action=? and a.created_date>=? and a.created_date<=? and (?='' OR a.connect_to_mobile_channel=?) and (?='' OR a.branch_code=?) and (?='' OR a.created_by=?) and is_from_mobile=0 ORDER BY a." + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        } else if (type.equals("MOBILE")) {
                            mainSql = "SELECT a.*, b.name branch, c.* FROM accounts a LEFT JOIN customers c on c.cust_no = a.customer_number inner join branches b on a.branch_code = b.code " + searchQuery + "and c.action=? and a.created_date>=? and a.created_date<=? and (?='' OR a.connect_to_mobile_channel=?) and (?='' OR a.branch_code=?) and (?='' OR a.created_by=?) and is_from_mobile=1 ORDER BY a." + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        }
                        totalRecordWithFilter = jdbcKycTemplate.queryForObject(countSql, new Object[]{fromDate, toDate, branchCode, branchCode, hasMobileChannel, hasMobileChannel, agent, agent, category, searchValue}, Integer.class);
                        results = this.jdbcKycTemplate.queryForList(mainSql, new Object[]{searchValue, category, fromDate, toDate, hasMobileChannel, hasMobileChannel, branchCode, branchCode, agent, agent});
                    } else {
                        mainSql = "SELECT a.*, b.name branch, c.* FROM accounts a LEFT JOIN customers c on c.cust_no = a.customer_number inner join branches b on a.branch_code = b.code " + searchQuery + "and a.created_date>=? and a.created_date<=? and (?='' OR a.connect_to_mobile_channel=?) and (?='' OR a.branch_code=?) and (?='' OR a.created_by=?) ORDER BY a." + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        if (type.equals("ASSISTED")) {
                            mainSql = "SELECT a.*, b.name branch, c.* FROM accounts a LEFT JOIN customers c on c.cust_no = a.customer_number inner join branches b on a.branch_code = b.code " + searchQuery + "and a.created_date>=? and a.created_date<=? and (?='' OR a.connect_to_mobile_channel=?) and (?='' OR a.branch_code=?) and (?='' OR a.created_by=?) and is_from_mobile=0 ORDER BY a." + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        } else if (type.equals("MOBILE")) {
                            mainSql = "SELECT a.*, b.name branch, c.* FROM accounts a LEFT JOIN customers c on c.cust_no = a.customer_number inner join branches b on a.branch_code = b.code " + searchQuery + "and a.created_date>=? and a.created_date<=? and (?='' OR a.connect_to_mobile_channel=?) and (?='' OR a.branch_code=?) and (?='' OR a.created_by=?) and is_from_mobile=1 ORDER BY a." + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        }
                        totalRecordWithFilter = jdbcKycTemplate.queryForObject(countSql, new Object[]{fromDate, toDate, branchCode, branchCode, hasMobileChannel, hasMobileChannel, agent, agent, searchValue}, Integer.class);
                        results = this.jdbcKycTemplate.queryForList(mainSql, new Object[]{searchValue, fromDate, toDate, hasMobileChannel, hasMobileChannel, branchCode, branchCode, agent, agent});
                    }
                }
            } else {
                totalRecordWithFilter = totalRecords;
                if (branchCode.equals("060")) {
                    if (!category.isEmpty()) {
                        mainSql = "SELECT a.*, b.name branch, c.* FROM accounts a LEFT JOIN customers c on c.cust_no = a.customer_number inner join branches b on a.branch_code = b.code WHERE c.action=? and a.created_date>=? and a.created_date<=? and (?='' OR a.connect_to_mobile_channel=?) and (?='' OR a.branch_code=?) and (?='' OR a.created_by=?) ORDER BY a." + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        if (type.equals("ASSISTED")) {
                            mainSql = "SELECT a.*, b.name branch, c.* FROM accounts a LEFT JOIN customers c on c.cust_no = a.customer_number inner join branches b on a.branch_code = b.code WHERE c.action=? and a.created_date>=? and a.created_date<=? and (?='' OR a.connect_to_mobile_channel=?) and (?='' OR a.branch_code=?) and (?='' OR a.created_by=?) and is_from_mobile=0 ORDER BY a." + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        } else if (type.equals("MOBILE")) {
                            mainSql = "SELECT a.*, b.name branch, c.* FROM accounts a LEFT JOIN customers c on c.cust_no = a.customer_number inner join branches b on a.branch_code = b.code WHERE c.action=? and a.created_date>=? and a.created_date<=? and (?='' OR a.connect_to_mobile_channel=?) and (?='' OR a.branch_code=?) and (?='' OR a.created_by=?) and is_from_mobile=1 ORDER BY a." + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        }
                        LOGGER.info(mainSql.replace("?", "'{}'"), category, fromDate, toDate, hasMobileChannel, hasMobileChannel, branch, branch, agent, agent);
                        results = this.jdbcKycTemplate.queryForList(mainSql, new Object[]{category, fromDate, toDate, hasMobileChannel, hasMobileChannel, branch, branch, agent, agent});
                    } else {
                        mainSql = "SELECT a.*, b.name branch, c.* FROM accounts a LEFT JOIN customers c on c.cust_no = a.customer_number inner join branches b on a.branch_code = b.code WHERE a.created_date>=? and a.created_date<=? and (?='' OR a.connect_to_mobile_channel=?) and (?='' OR a.branch_code=?) and (?='' OR a.created_by=?) ORDER BY a." + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        if (type.equals("ASSISTED")) {
                            mainSql = "SELECT a.*, b.name branch, c.* FROM accounts a LEFT JOIN customers c on c.cust_no = a.customer_number inner join branches b on a.branch_code = b.code WHERE a.created_date>=? and a.created_date<=? and (?='' OR a.connect_to_mobile_channel=?) and (?='' OR a.branch_code=?) and (?='' OR a.created_by=?) and is_from_mobile=0 ORDER BY a." + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        } else if (type.equals("MOBILE")) {
                            mainSql = "SELECT a.*, b.name branch, c.* FROM accounts a LEFT JOIN customers c on c.cust_no = a.customer_number inner join branches b on a.branch_code = b.code WHERE a.created_date>=? and a.created_date<=? and (?='' OR a.connect_to_mobile_channel=?) and (?='' OR a.branch_code=?) and (?='' OR a.created_by=?) and is_from_mobile=1 ORDER BY a." + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        }
                        LOGGER.info(mainSql.replace("?", "'{}'"), fromDate, toDate, hasMobileChannel, hasMobileChannel, branch, branch, agent, agent);
                        results = this.jdbcKycTemplate.queryForList(mainSql, new Object[]{fromDate, toDate, hasMobileChannel, hasMobileChannel, branch, branch, agent, agent});
                    }
                } else {
                    if (!category.isEmpty()) {
                        mainSql = "SELECT a.*, b.name branch, c.* FROM accounts a LEFT JOIN customers c on c.cust_no = a.customer_number inner join branches b on a.branch_code = b.code WHERE c.action=? and a.created_date>=? and a.created_date<=? and (?='' OR a.connect_to_mobile_channel=?) and (?='' OR a.branch_code=?) and (?='' OR a.created_by=?) ORDER BY a." + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        if (type.equals("ASSISTED")) {
                            mainSql = "SELECT a.*, b.name branch, c.* FROM accounts a LEFT JOIN customers c on c.cust_no = a.customer_number inner join branches b on a.branch_code = b.code WHERE c.action=? and a.created_date>=? and a.created_date<=? and (?='' OR a.connect_to_mobile_channel=?) and (?='' OR a.branch_code=?) and (?='' OR a.created_by=?) and is_from_mobile=0 ORDER BY a." + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        } else if (type.equals("MOBILE")) {
                            mainSql = "SELECT a.*, b.name branch, c.* FROM accounts a LEFT JOIN customers c on c.cust_no = a.customer_number inner join branches b on a.branch_code = b.code WHERE c.action=? and a.created_date>=? and a.created_date<=? and (?='' OR a.connect_to_mobile_channel=?) and (?='' OR a.branch_code=?) and (?='' OR a.created_by=?) and is_from_mobile=1 ORDER BY a." + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        }
                        LOGGER.info(mainSql.replace("?", "'{}'"), category, fromDate, toDate, hasMobileChannel, hasMobileChannel, branchCode, branchCode, agent, agent);
                        results = this.jdbcKycTemplate.queryForList(mainSql, new Object[]{category, fromDate, toDate, hasMobileChannel, hasMobileChannel, branchCode, branchCode, agent, agent});
                    } else {
                        mainSql = "SELECT a.*, b.name branch, c.* FROM accounts a LEFT JOIN customers c on c.cust_no = a.customer_number inner join branches b on a.branch_code = b.code WHERE a.created_date>=? and a.created_date<=? and (?='' OR a.connect_to_mobile_channel=?) and (?='' OR a.branch_code=?) and (?='' OR a.created_by=?) ORDER BY a." + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        if (type.equals("ASSISTED")) {
                            mainSql = "SELECT a.*, b.name branch, c.* FROM accounts a LEFT JOIN customers c on c.cust_no = a.customer_number inner join branches b on a.branch_code = b.code WHERE a.created_date>=? and a.created_date<=? and (?='' OR a.connect_to_mobile_channel=?) and (?='' OR a.branch_code=?) and (?='' OR a.created_by=?) and is_from_mobile=0 ORDER BY a." + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        } else if (type.equals("MOBILE")) {
                            mainSql = "SELECT a.*, b.name branch, c.* FROM accounts a LEFT JOIN customers c on c.cust_no = a.customer_number inner join branches b on a.branch_code = b.code WHERE a.created_date>=? and a.created_date<=? and (?='' OR a.connect_to_mobile_channel=?) and (?='' OR a.branch_code=?) and (?='' OR a.created_by=?) and is_from_mobile=1 ORDER BY a." + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        }
                        LOGGER.info(mainSql.replace("?", "'{}'"), fromDate, toDate, hasMobileChannel, hasMobileChannel, branchCode, branchCode, agent, agent);
                        results = this.jdbcKycTemplate.queryForList(mainSql, new Object[]{fromDate, toDate, hasMobileChannel, hasMobileChannel, branchCode, branchCode, agent, agent});
                    }
                }
            }
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        return "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordWithFilter + "\",\"aaData\":" + jsonString + "}";
    }

    public String getAgentsInfo(String branchCode, String isEnabled, String isAgent, String category, String draw, String start,
                                String rowPerPage, String searchValue, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results;
        String mainSql;
        int totalRecordWithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        if (category == null) {
            category = "";
        }
        try {
            mainSql = "SELECT count(id) FROM users u where (?='' OR branch_code=?) AND " +
                    "(?='' OR enabled = ?) AND (?='' OR is_agent = ?) AND (?='' OR category=?)";
            totalRecords = jdbcKycTemplate.queryForObject(mainSql, new Object[]{branchCode, branchCode, isEnabled,
                    isEnabled, isAgent, isAgent, category, category}, Integer.class);
            String searchQuery;
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                searchQuery = " WHERE concat(u.created_by,' ',u.created_date,' ',u.last_modified_by,' ',u.last_modified_date," +
                        "' ',u.rec_status,' ',device_id,' ',email,' ',enabled,' ',username,' ',user_type,' '," +
                        "branch_code,' ',first_login,' ',firstname,' ',lastname,' ',middlename,' ',phone) LIKE ? " +
                        "AND (?='' OR branch_code=?) AND (?='' OR enabled = ?) AND (?='' OR is_agent = ?) AND (?='' OR category=?) ";
                totalRecordWithFilter = jdbcKycTemplate.queryForObject("SELECT count(id) FROM users u" + searchQuery,
                        new Object[]{searchValue, branchCode, branchCode, isEnabled, isEnabled, isAgent, isAgent,
                                category, category}, Integer.class);
                mainSql = "SELECT * FROM users u inner join branches b on u.branch_code = b.code " + searchQuery + "ORDER BY " + columnName + " " + columnSortOrder +
                        " OFFSET " + start + "ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                results = this.jdbcKycTemplate.queryForList(mainSql, searchValue, branchCode, branchCode,
                        isEnabled, isEnabled, isAgent, isAgent, category, category);
            } else {
                totalRecordWithFilter = totalRecords;
                mainSql = "SELECT * FROM users inner join branches b on users.branch_code = b.code where (?='' OR branch_code=?) AND (?='' OR enabled = ?) AND " +
                        "(?='' OR is_agent = ?) AND (?='' OR category=?) ORDER BY " + columnName + " " +
                        columnSortOrder + " OFFSET " + start + "ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                LOGGER.info(mainSql.replace("?", "'{}'"), branchCode, branchCode, isEnabled, isEnabled,
                        isAgent, isAgent, category, category);
                results = this.jdbcKycTemplate.queryForList(mainSql, branchCode, branchCode, isEnabled, isEnabled,
                        isAgent, isAgent, category, category);
            }
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        return "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordWithFilter + "\",\"aaData\":" + jsonString + "}";
    }

    public String getCustomerKycAndAcctDetails(String trackingNo, String username, String branchCode) {
        String reportFileTemplate = "/iReports/kyc/kyc_customer_details.jasper";
        Integer totalRecords;
        boolean hasAccount = false;
        boolean hasRim = false;
        boolean isEnrolledMobileChannel = false;
        Map<String, Object> result;
        String recentAccountNo = "";
        Connection conn = null;

        String acctSql = "SELECT top 1 customers.cust_no as customer_no, connect_to_mobile_channel FROM customers LEFT JOIN accounts on accounts.customer_number=customers.cust_no where customers.id = ? order by accounts.id desc";
        try {
            result = jdbcKycTemplate.queryForMap(acctSql, new Object[]{trackingNo});
            String rimNo = (String) result.getOrDefault("customer_no", "");
            String isEnrolled = (String) result.getOrDefault("connect_to_mobile_channel", "");
            if (rimNo.length() == 10) hasRim = true;
            if (isEnrolled != null && isEnrolled.equals("Yes")) isEnrolledMobileChannel = true;
        } catch(DataAccessException ex) {
            ex.printStackTrace();
            LOGGER.error("DataAccessException is {}", ex);

        }

        try {
            String countSql = "SELECT count(accounts.id) as count FROM accounts INNER JOIN customers on accounts.customer_number=customers.cust_no where customers.id = ? and accounts.cbs_response_code = 0";
            totalRecords = jdbcKycTemplate.queryForObject(countSql, new Object[]{trackingNo}, Integer.class);
            if (totalRecords != null && totalRecords > 0) {
                hasAccount = true;
                countSql = "SELECT top 1 account_no FROM accounts INNER JOIN customers on accounts.customer_number=" +
                        "customers.cust_no where customers.id = ? and accounts.cbs_response_code = 0 order by accounts.id desc";
                recentAccountNo = jdbcKycTemplate.queryForObject(countSql, new Object[]{trackingNo}, String.class);
            }
        } catch (DataAccessException ex) {
            ex.printStackTrace();
            LOGGER.error("DataAccessException is {}", ex);
        }

        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("PRINTED_BY", username);
            parameters.put("PRINTED_AT", DateUtil.now("dd/MM/yyyy HH:mm:ss"));
            parameters.put("TRACKING_NO", trackingNo);
            parameters.put("ACCOUNT_NO", recentAccountNo);
            conn = kycDatasource.getConnection();

            LOGGER.error("reportFileTemplate is {}", reportFileTemplate);
            JasperPrint print = jasperService.jasperPrint(reportFileTemplate, parameters, conn);
            LOGGER.error("JasperPrint is {}", print);

            conn.close();
            return "{\"hasRim\":" + hasRim + ",\"hasAccount\":" + hasAccount + ",\"isEnrolled\":" + isEnrolledMobileChannel + ",\"jasper\":\"" + Base64.encodeBase64String(jasperService.exportPdfToStream(print).toByteArray()) + "\"}";
        } catch (IOException ex) {
            LOGGER.error(null, ex);
            return "{\"hasRim\":" + hasRim + ",\"hasAccount\":" + hasAccount + ",\"isEnrolled\":" + isEnrolledMobileChannel + ",\"jasper\":" + null + "}";
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            Logger.getLogger(KycRepo.class.getName()).log(Level.SEVERE, null, ex);
            return "{\"hasRim\":" + hasRim + ",\"hasAccount\":" + hasAccount + ",\"isEnrolled\":" + isEnrolledMobileChannel + ",\"jasper\":" + null + "}";
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String getAcctDetails(String customerNo, String accountNo, String username) {
        String reportFileTemplate = "/iReports/kyc/acct_details.jasper";
        boolean hasAccount = true;
        boolean hasRim = true;
        boolean isEnrolledMobileChannel = false;
        Map<String, Object> result;
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("PRINTED_BY", username);
        parameters.put("PRINTED_AT", DateUtil.now("dd/MM/yyyy HH:mm:ss"));
        parameters.put("CUSTOMER_NO", customerNo);

        String acctSql = "select c.id, c.cust_no, c.id_number, c.first_name, c.middle_name, c.last_name, c.gender, c.dob, " +
                "c.branch_code, c.email_address, c.residence_region, c.residence_district, c.residence_area, c.nationality, " +
                "c.birth_place, c.birth_region, c.birth_district, c.residence_house_no, c.phone_number, c.email_address, " +
                "c.occupation, c.employer, c.employment_status, c.income_source, c.monthly_income, " +
                "photo = (select cast(c.photo as varchar(max)) FOR XML PATH(''), BINARY BASE64), " +
                "signature = (select cast(c.signature as varchar(max)) FOR XML PATH(''), BINARY BASE64), " +
                "a.created_by, a.account_no, a.account_title, a.connect_to_mobile_channel, a.created_date " +
                "from accounts a inner join customers c on cust_no = customer_number where customer_number = ? and " +
                "account_no = ? order by id desc";
        // check if customer exists
        String customerSql = "select * from customers c where c.cust_no = ?";
        try {
            LOGGER.info(customerSql.replace("?", "'{}'"), customerNo);
            result = jdbcKycTemplate.queryForMap(customerSql, customerNo);
            if (!result.isEmpty()){
                LOGGER.info(acctSql.replace("?", "'{}'"), customerNo, accountNo);
                result = jdbcKycTemplate.queryForMap(acctSql, customerNo, accountNo);

                parameters.put("ACCOUNT_NO", accountNo);
                parameters.put("ID_NO", result.getOrDefault("id_number", ""));
                parameters.put("FIRST_NAME", result.getOrDefault("first_name", ""));
                parameters.put("MIDDLE_NAME", result.getOrDefault("middle_name", ""));
                parameters.put("LAST_NAME", result.getOrDefault("last_name", ""));
                parameters.put("OTHER_NAMES", result.getOrDefault("account_title", ""));
                parameters.put("GENDER", result.getOrDefault("gender", ""));
                parameters.put("DOB", result.getOrDefault("dob", ""));
                parameters.put("MOBILE", result.getOrDefault("phone_number", ""));
                parameters.put("EMAIL", result.getOrDefault("email_address", ""));
                parameters.put("PHOTO", result.getOrDefault("photo", ""));
                parameters.put("SIGNATURE", result.getOrDefault("signature", ""));
                parameters.put("PLACE_OF_BIRTH", result.getOrDefault("birth_place", ""));
                parameters.put("NATIONALITY", result.getOrDefault("nationality", ""));
                parameters.put("BIRTH_REGION", result.getOrDefault("birth_region", ""));
                parameters.put("BIRTH_DISTRICT", result.getOrDefault("birth_district", ""));
                parameters.put("RESIDENT_REGION", result.getOrDefault("residence_region", ""));
                parameters.put("RESIDENT_DISTRICT", result.getOrDefault("residence_district", ""));
                parameters.put("RESIDENT_WARD", result.getOrDefault("residence_ward", ""));
                parameters.put("RESIDENT_STREET", result.getOrDefault("residence_area", ""));
                parameters.put("RESIDENT_HOUSE_NO", result.getOrDefault("residence_house_no", ""));
                parameters.put("OCCUPATION", result.getOrDefault("occupation", ""));
                parameters.put("POSTAL_ADDRESS", result.getOrDefault("postal_address", ""));
                parameters.put("EMPLOYER", result.getOrDefault("employer", ""));
                parameters.put("EMPLOYMENT_STATUS", result.getOrDefault("employment_status", ""));
                parameters.put("INCOME_SOURCE", result.getOrDefault("income_source", ""));
                parameters.put("INCOME_RANGE", result.getOrDefault("monthly_income", ""));
                parameters.put("CREATED_BY", result.getOrDefault("created_by", ""));
                parameters.put("CREATED_DATE", Timestamp.valueOf(result.getOrDefault("created_date", "") + ""));
                String isEnrolled = (String) result.getOrDefault("connect_to_mobile_channel", "");
                String branchSql = "select * from branches where code = ?";
                Map<String, Object> branchRes = jdbcKycTemplate.queryForMap(branchSql, result.getOrDefault(
                        "branch_code", ""));
                parameters.put("BRANCH_NAME", branchRes.getOrDefault("name", ""));
                parameters.put("BRANCH_CODE", branchRes.getOrDefault("code", ""));
                if (isEnrolled != null && isEnrolled.equals("Yes")) isEnrolledMobileChannel = true;
                String sql = "select cb.handtype, cb.finger1, cb.finger2, cb.finger3, cb.finger4 from " +
                        "customer_biometric cb where id_number = ?";
                try {
                    Map<String, Object> response = jdbcKycTemplate.queryForMap(sql, result.getOrDefault(
                            "id_number_formatted", ""));
                    parameters.put("HAND_TYPE", response.getOrDefault("handtype", ""));
                    parameters.put("FINGER_1", response.getOrDefault("finger1", ""));
                    parameters.put("FINGER_2", response.getOrDefault("finger2", ""));
                    parameters.put("FINGER_3", response.getOrDefault("finger3", ""));
                    parameters.put("FINGER_4", response.getOrDefault("finger4", ""));
                } catch (EmptyResultDataAccessException e) {
                    parameters.put("HAND_TYPE", "");
                    parameters.put("FINGER_1", "");
                    parameters.put("FINGER_2", "");
                    parameters.put("FINGER_3", "");
                    parameters.put("FINGER_4", "");
                }
            } else {
                throw new EmptyResultDataAccessException("No customer record found in local repository", 1);
            }
        } catch(DataAccessException ex) {
            ex.printStackTrace();
            try {
                String customerInfo = getCustomerInfo(customerNo);
                QueryCustomerResponse queryCustomerResponse = jacksonMapper.readValue(customerInfo, QueryCustomerResponse.class);

                acctSql = "SELECT cu.CUST_ID, cu.CUST_NO, p.first_nm as firstName, p.middle_nm as middleName, " +
                        "p.last_nm as lastName, p.birth_dt as dob, n.nationality_desc as nationality, c.cntry_nm as placeOfBirth, " +
                        "p.SOCIAL_SECURITY_NO as nidaNumber, g.REF_DESC as gender, " +
                        "(select STATE from ADDRESS where ADDR_ID=ca.ADDR_ID) as region, " +
                        "(select CITY from ADDRESS where ADDR_ID=ca.ADDR_ID) as district, " +
                        "(select ADDR_LINE_2 from ADDRESS where ADDR_ID=ca.ADDR_ID) as ward, " +
                        "(select ADDR_LINE_3 from ADDRESS where ADDR_ID=ca.ADDR_ID) as street, " +
                        "(select ADDR_LINE_4 from ADDRESS where ADDR_ID=ca.ADDR_ID) as houseNo, " +
                        "(select ADDR_LINE_1||' '||ADDR_LINE_2||' '|| CITY||' '|| STATE from ADDRESS where ADDR_ID=ca.ADDR_ID) as physicalAddress, " +
                        "(select max(contact) from customer_contact_mode where cust_id = p.cust_id and contact_mode_id " +
                        "not in ('234','235','241','261','262','263','264','265','266','271','272','718900','718910','718920')) as phoneNumber, " +
                        "(select max(contact) from customer_contact_mode where cust_id = p.cust_id and contact_mode_id " +
                        "in ('234','235','241','261','262','263','264','265','266','271','272','718900','718910','718920')) as email, " +
                        "CASE WHEN (SELECT MAX(CUST_EMP_ID) FROM CUSTOMER_EMPLOYMENT ce WHERE ce.CUST_ID = cu.CUST_ID) " +
                        "IS NULL THEN 'UNEMPLOYED' ELSE 'EMPLOYED' END AS employmentStatus, " +
                        "(SELECT MAX(EMPLOYER_NM) FROM CUSTOMER_EMPLOYMENT ce WHERE ce.CUST_ID = cu.CUST_ID) AS employer, " +
                        "(SELECT or2.OCCUPATION_DESC FROM OCCUPATION_REF or2 WHERE or2.OCCUPATION_ID = " +
                        "(SELECT MAX(ce.OCCUPATION_ID) FROM CUSTOMER_EMPLOYMENT ce WHERE ce.CUST_ID = cu.CUST_ID)) AS occupation " +
                        "from person p, nationality_ref n, country c, gender_ref g, CUSTOMER_ADDRESS ca, customer cu " +
                        "where p.NATIONALITY_ID = n.NATIONALITY_ID and p.CNTRY_OF_BIRTH_ID = c.cntry_id and " +
                        "g.ref_key = p.GENDER_TY and p.CUST_ID = ca.CUST_ID AND ca.CUST_ID = cu.CUST_ID AND cu.CUST_NO = ?";

                LOGGER.info(acctSql.replace("?", "'{}'"), customerNo);
                result = jdbcLiveTemplate.queryForMap(acctSql, customerNo);
                parameters.put("ACCOUNT_NO", accountNo);
                parameters.put("ID_NO", result.getOrDefault("nidaNumber", ""));
                parameters.put("FIRST_NAME", result.getOrDefault("firstName", ""));
                parameters.put("MIDDLE_NAME", result.getOrDefault("middleName", ""));
                parameters.put("LAST_NAME", result.getOrDefault("lastName", ""));
                parameters.put("OTHER_NAMES", queryCustomerResponse.getAccountName());
                parameters.put("GENDER", result.getOrDefault("gender", ""));
                parameters.put("DOB", queryCustomerResponse.getDob());
                parameters.put("MOBILE", result.getOrDefault("phoneNumber", ""));
                parameters.put("EMAIL", result.getOrDefault("email", ""));
                parameters.put("PHOTO", queryCustomerResponse.getPhoto());
                parameters.put("SIGNATURE", queryCustomerResponse.getSignature());
                parameters.put("PLACE_OF_BIRTH", result.getOrDefault("placeOfBirth", ""));
                parameters.put("NATIONALITY", result.getOrDefault("nationality", ""));
                parameters.put("BIRTH_REGION", result.getOrDefault("birthRegion", ""));//*not present
                parameters.put("BIRTH_DISTRICT", result.getOrDefault("birthDistrict", ""));//*not present
                parameters.put("RESIDENT_REGION", result.getOrDefault("region", ""));
                parameters.put("RESIDENT_DISTRICT", result.getOrDefault("district", ""));
                parameters.put("RESIDENT_WARD", result.getOrDefault("ward", ""));
                parameters.put("RESIDENT_STREET", result.getOrDefault("street", ""));
                parameters.put("RESIDENT_HOUSE_NO", result.getOrDefault("houseNo", ""));
                parameters.put("POSTAL_ADDRESS", result.getOrDefault("physicalAddress", ""));
                parameters.put("OCCUPATION", result.getOrDefault("occupation", ""));
                parameters.put("EMPLOYER", result.getOrDefault("employer", ""));
                parameters.put("EMPLOYMENT_STATUS", result.getOrDefault("employmentStatus", ""));
                parameters.put("INCOME_SOURCE", "");
                parameters.put("BRANCH_NAME", queryCustomerResponse.getBuName());
                parameters.put("BRANCH_CODE", queryCustomerResponse.getBuCode());
                parameters.put("HAND_TYPE", "");
                parameters.put("FINGER_1", "");
                parameters.put("FINGER_2", "");
                parameters.put("FINGER_3", "");
                parameters.put("FINGER_4", "");

                acctSql = "select * from accounts where account_no = ? order by id desc";
                Map<String, Object> response = jdbcKycTemplate.queryForMap(acctSql, accountNo);
                if (response.isEmpty()) {
                    parameters.put("INCOME_RANGE", result.getOrDefault("monthly_income", ""));
                    parameters.put("CREATED_BY", result.getOrDefault("created_by", ""));
                    parameters.put("CREATED_DATE", result.getOrDefault("created_date", ""));
                }
            } catch (JsonProcessingException | DataAccessException e) {
                e.printStackTrace();
            }
        }
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            LOGGER.info("{}", entry.getKey() + ":" + entry.getValue());
        }
        try {
            JasperPrint print;
            try (InputStream jasperStream = this.getClass().getResourceAsStream(reportFileTemplate)) {
                JasperReport jasperReport = (JasperReport) JRLoader.loadObject(jasperStream);
                print = JasperFillManager.fillReport(jasperReport, parameters, new JREmptyDataSource());
            }
            return "{\"hasRim\":" + hasRim + ",\"hasAccount\":" + hasAccount + ",\"isEnrolled\":" + isEnrolledMobileChannel + ",\"jasper\":\"" + Base64.encodeBase64String(jasperService.exportPdfToStream(print).toByteArray()) + "\"}";
        } catch (IOException ex) {
            Logger.getLogger(KycRepo.class.getName()).log(Level.SEVERE, null, ex);
            return "{\"hasRim\":" + hasRim + ",\"hasAccount\":" + hasAccount + ",\"isEnrolled\":" + isEnrolledMobileChannel + ",\"jasper\":" + null + "}";
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            Logger.getLogger(KycRepo.class.getName()).log(Level.SEVERE, null, ex);
            return "{\"hasRim\":" + hasRim + ",\"hasAccount\":" + hasAccount + ",\"isEnrolled\":" + isEnrolledMobileChannel + ",\"jasper\":" + null + "}";
        }
    }

    public String viewCustomerAttachment(String trackingNo, String username) {
        String reportFileTemplate = "/iReports/kyc/kyc_attachments.jasper";
        Connection conn = null;
        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("PRINTED_BY", username);
            parameters.put("PRINTED_AT", DateUtil.now("dd/MM/yyyy HH:mm:ss"));
            parameters.put("customer_id", Long.parseLong(trackingNo));
            conn = kycDatasource.getConnection();
            JasperPrint print = jasperService.jasperPrint(reportFileTemplate, parameters, conn);
            conn.close();
            return "{\"jasper\":\"" + Base64.encodeBase64String(jasperService.exportPdfToStream(print).toByteArray()) + "\"}";
        } catch (IOException ex) {
            Logger.getLogger(KycRepo.class.getName()).log(Level.SEVERE, null, ex);
            return "\"jasper\":" + null + "}";
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            Logger.getLogger(KycRepo.class.getName()).log(Level.SEVERE, null, ex);
            return "\"jasper\":" + null + "}";
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String getKYCCustomerDetails(String trackingNo) {
        Map<String, Object> result;
        String jsonString;

        String acctSql = "SELECT top 1 * FROM customers where customers.id = ?";
        try {
            result = jdbcKycTemplate.queryForMap(acctSql, new Object[]{trackingNo});
            jsonString = this.jacksonMapper.writeValueAsString(result);
        } catch(EmptyResultDataAccessException | JsonProcessingException ex) {
            ex.printStackTrace();
            jsonString = null;
        }
        return jsonString;
    }

    public String editKYCCustomerDetails(String trackingNo, String username, String UPDATE_SQL, ArrayList<Object> args) {
        try {
            Object[] a = new Object[]{username, DateUtil.now()};
            Object[] b = Stream.concat(Arrays.stream(a), Arrays.stream(args.toArray())).toArray();
            Object[] c = new Object[]{trackingNo};
            Object[] d = Stream.concat(Arrays.stream(b), Arrays.stream(c)).toArray();
            LOGGER.info(UPDATE_SQL.replace("?","'{}'"), d);
            int updateResult = jdbcKycTemplate.update(UPDATE_SQL, d);
            if (updateResult == 1) {
                return "{\"responseCode\":\"0\", \"message\":\"Edit successfully!\"}";
            } else {
                return "{\"responseCode\":\"99\", \"message\":\"Oops! Something went wrong, please try again!\"}";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"responseCode\":\"99\", \"message\":\"Oops! Something went wrong, please try again!\"}";
        }
    }

    public String getGroupsInfo(String branchCode, String category, String draw, String start,
                                String rowPerPage, String searchValue, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results;
        String mainSql;
        int totalRecordWithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        System.out.println("The value for draw is: " + draw);
        try {
            if (branchCode.equals("060")) {
                mainSql = "SELECT count(id) FROM groups where category=? ";
                totalRecords = jdbcKycTemplate.queryForObject(mainSql, new Object[]{category}, Integer.class);
            } else {
                mainSql = "SELECT count(id) FROM groups where branch_code=? and category=? ";
                totalRecords = jdbcKycTemplate.queryForObject(mainSql, new Object[]{branchCode, category}, Integer.class);
            }
            String searchQuery;
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                if (branchCode.equals("060"))
                    searchQuery = " WHERE concat(created_by,' ',created_date,' ',last_modified_by,' ',last_modified_date,' ',rec_status,' ',name,' ',trade_name,' ',registration_no,' ',cbs_response_code,' ',cbs_response_message,' ',category,' ',business_type,' ',signing_powers,' ',email_address,' ',postal_address,' ',other_services,' ',tin_no,' ',registered_address,' ',currency,' ',expected_monthly_income,' ',tracking_no,' ',business_physical_address,' ',area_street,' ',office_phone_no,' ',company_head_office_address,' ',house_no,' ',fax_no,' ',branch_code,' ',action_comments,' ',action_status) LIKE ? and category=?";
                else
                    searchQuery = " WHERE concat(created_by,' ',created_date,' ',last_modified_by,' ',last_modified_date,' ',rec_status,' ',name,' ',trade_name,' ',registration_no,' ',cbs_response_code,' ',cbs_response_message,' ',category,' ',business_type,' ',signing_powers,' ',email_address,' ',postal_address,' ',other_services,' ',tin_no,' ',registered_address,' ',currency,' ',expected_monthly_income,' ',tracking_no,' ',business_physical_address,' ',area_street,' ',office_phone_no,' ',company_head_office_address,' ',house_no,' ',fax_no,' ',branch_code,' ',action_comments,' ',action_status) LIKE ?  and  branch_code=? and category=?";
                totalRecordWithFilter = jdbcKycTemplate.queryForObject("SELECT count(id) FROM groups " + searchQuery, new Object[]{searchValue, branchCode, category}, Integer.class);
                mainSql = "SELECT * FROM groups " + searchQuery + "  ORDER BY " + columnName + " " + columnSortOrder + " OFFSET " + start + "ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                results = this.jdbcKycTemplate.queryForList(mainSql, new Object[]{searchValue, branchCode, category});
            } else {
                totalRecordWithFilter = totalRecords;
                if (branchCode.equals("060")) {
                    mainSql = "SELECT * FROM groups where category=? ORDER BY " + columnName + " " + columnSortOrder + " OFFSET " + start + "ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                    results = this.jdbcKycTemplate.queryForList(mainSql, new Object[]{category});
                } else {
                    mainSql = "SELECT * FROM groups where branch_code=? and category=? ORDER BY " + columnName + " " + columnSortOrder + " OFFSET " + start + "ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                    results = this.jdbcKycTemplate.queryForList(mainSql, new Object[]{branchCode, category});
                }
            }
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        return "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordWithFilter + "\",\"aaData\":" + jsonString + "}";
    }

    public String getGroupSignatories(String id, String username, String branchCode) {
        List<Map<String, Object>> results;
        String jsonString = null;
        int totalRecords;
        String mainSql = "SELECT * FROM customers a where group_id=? and branch_code=?";
        if (branchCode.equals("060")) {
            mainSql = "SELECT * FROM customers a where group_id=?";
            results = this.jdbcKycTemplate.queryForList(mainSql, new Object[]{id});
            totalRecords = jdbcKycTemplate.queryForObject("SELECT count(id) FROM customers where group_id=? ", new Object[]{id}, Integer.class);
        } else {
            results = this.jdbcKycTemplate.queryForList(mainSql, new Object[]{id, branchCode});
            totalRecords = jdbcKycTemplate.queryForObject("SELECT count(id) FROM customers where group_id=? and branch_code=?", new Object[]{id, branchCode}, Integer.class);
        }
        try {
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return "{\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecords + "\",\"aaData\":" + jsonString + "}";
    }

    public String getGroupKycDetails(String exporterFileType, HttpServletResponse response,
                                     String destName, String trackingNo, String username, String branchCode) {
        String reportFileTemplate = "/iReports/kyc/kyc_group_details.jasper";
        Connection conn = null;
        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("TRACKING_NO", trackingNo);
            parameters.put("PRINTED_BY", username);
            parameters.put("BRANCH_CODE", branchCode);
            parameters.put("logo", "tcb.png");
            conn = kycDatasource.getConnection();
            JasperPrint print = jasperService.jasperPrint(reportFileTemplate, parameters, conn);
            conn.close();
            return jasperService.exportFileOption(print, exporterFileType, response, destName);
        } catch (IOException ex) {
            Logger.getLogger(KycRepo.class.getName()).log(Level.SEVERE, null, ex);
            return null;

        } catch (Exception ex) {
            LOGGER.info(null, ex);
            Logger.getLogger(KycRepo.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String enableAgent(String userId, String username, String createDate) {
        String mainSql = "UPDATE users set enabled=1, rec_status='A', last_modified_by=?, last_modified_date=? where id=?";
        int results = this.jdbcKycTemplate.update(mainSql, new Object[]{username, createDate, userId});
        if (results > 0) {
            return "{\"success\":\"true\"}";
        } else {
            return "{\"success\":\"false\"}";
        }
    }

    public String disableAgent(String userId, String username, String now) {
        String mainSql = "UPDATE users set enabled=0, rec_status='B', last_modified_by=?, last_modified_date=? where id=?";
        int results = this.jdbcKycTemplate.update(mainSql, new Object[]{username, now, userId});
        if (results > 0) {
            return "{\"success\":\"true\"}";
        } else {
            return "{\"success\":\"false\"}";
        }
    }

    public String createRIMWithNida(String rowId, String url) {
        String mainSql = "SELECT * FROM customers INNER JOIN branches on customers.branch_code=branch.code WHERE id = ?";
        List<Map<String, Object>> results = this.jdbcKycTemplate.queryForList(mainSql, new Object[]{rowId});

        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setIdType((String) results.get(0).getOrDefault("id_type", ""));
        request.setIdNumber((String) results.get(0).getOrDefault("id_number", ""));
        request.setFirstName((String) results.get(0).getOrDefault("first_name", ""));
        request.setMiddleName((String) results.get(0).getOrDefault("middle_name", ""));
        request.setLastName((String) results.get(0).getOrDefault("last_name", ""));
        request.setBranchCode((String) results.get(0).getOrDefault("branch_code", ""));
        request.setGender((String) results.get(0).getOrDefault("gender", ""));
        request.setTitle((String) results.get(0).getOrDefault("title", ""));
        request.setPhoneNumber((String) results.get(0).getOrDefault("phone_number", ""));
        request.setEmailAddress((String) results.get(0).getOrDefault("email_address", ""));
        request.setPhoto((String) results.get(0).getOrDefault("photo", ""));
        request.setSignature((String) results.get(0).getOrDefault("signature", ""));
        request.setDob((String) results.get(0).getOrDefault("dob", ""));
        request.setMaritalStatus((String) results.get(0).getOrDefault("marital_status", ""));
        request.setBirthRegion((String) results.get(0).getOrDefault("birth_region", ""));
        request.setBirthDistrict((String) results.get(0).getOrDefault("birth_district", ""));
        request.setBirthPlace((String) results.get(0).getOrDefault("birth_place", ""));
        request.setBirthWard((String) results.get(0).getOrDefault("birth_ward", ""));
        request.setResidenceRegion((String) results.get(0).getOrDefault("residence_region", ""));
        request.setResidenceDistrict((String) results.get(0).getOrDefault("residence_district", ""));
        request.setResidenceArea((String) results.get(0).getOrDefault("residence_area", ""));
        request.setResidenceHouseNo((String) results.get(0).getOrDefault("residence_house_no", ""));
        String city = (String) results.get(0).getOrDefault("city", "");
        if (city.isEmpty())
            request.setCity("DAR ES SALAAM");
        else
            request.setCity(city);
        request.setCustomerTypeId("718950");
        request.setCustomerTypeCat("76");
        request.setBranchCode((String) results.get(0).getOrDefault("branch_code", ""));
        request.setBranchId((String) results.get(0).getOrDefault("branch_id", ""));
        request.setBranchName((String) results.get(0).getOrDefault("branch_name", ""));
        request.setMonthlyIncome((String) results.get(0).getOrDefault("monthly_income", ""));
        request.setChequeNumber((String) results.get(0).getOrDefault("cheque_number", ""));
        request.setAmlServiceId((Integer) results.get(0).getOrDefault("aml_service_id", 21));
        request.setDisabilityStatusId((Integer) results.get(0).getOrDefault("disability_status_id", 3000));
        request.setBusinessActivityId((String) results.get(0).getOrDefault("business_activity_id", 21));
        request.setOpeningReasonId((String) results.get(0).getOrDefault("opening_reason_id", "549"));
        request.setIsPE((Boolean) results.get(0).getOrDefault("is_pe", false));

        String response = HttpClientService.sendRIMData(request, url);
        CreateCustomerResponse cbsResp = new Gson().fromJson(response, CreateCustomerResponse.class);

        //update after getting response
        String updateSql = "UPDATE customers SET cbs_response_code = ?, cbs_response_message = ?, cust_no = ?, action = ?, action_status = ?, action_comments = ?";

        if (cbsResp.getResponseCode().equals("0")) {
            results = this.jdbcKycTemplate.queryForList(updateSql, new Object[]{cbsResp.getResponseCode(), cbsResp.getMessage(), cbsResp.getCustomerRim(), "C", "Request completed successfully"});
            try {
                this.jacksonMapper.writeValueAsString(results);
                return "{\"success\":true, \"rim\":\"" + cbsResp.getCustomerRim() + "\", \"account\":\"" + cbsResp.getAccountNo() + "\", \"reference\":\"" + cbsResp.getReference() + "\", \"message\": \"" + cbsResp.getMessage() + "\"}";
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return "{\"success\": false, \"message\": \"Failed to create RIM\"}";
            }
        } else {
            results = this.jdbcKycTemplate.queryForList(updateSql, new Object[]{cbsResp.getResponseCode(), cbsResp.getMessage(), cbsResp.getCustomerRim(), "F", "Failed to create RIM"});
            try {
                this.jacksonMapper.writeValueAsString(results);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return "{\"success\": false, \"message\": \"Failed to create RIM\"}";
        }
    }

    public String createRIMWithoutNida(String rowId, String url) {
        String mainSql = "SELECT * FROM customers INNER JOIN branches on customers.branch_code=branch.code WHERE id = ?";
        List<Map<String, Object>> results = this.jdbcKycTemplate.queryForList(mainSql, new Object[]{rowId});

        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setIdType((String) results.get(0).getOrDefault("id_type", ""));
        request.setIdNumber((String) results.get(0).getOrDefault("id_number", ""));
        request.setFirstName((String) results.get(0).getOrDefault("first_name", ""));
        request.setMiddleName((String) results.get(0).getOrDefault("middle_name", ""));
        request.setLastName((String) results.get(0).getOrDefault("last_name", ""));
        request.setBranchCode((String) results.get(0).getOrDefault("branch_code", ""));
        request.setGender((String) results.get(0).getOrDefault("gender", ""));
        request.setTitle((String) results.get(0).getOrDefault("title", ""));
        request.setPhoneNumber((String) results.get(0).getOrDefault("phone_number", ""));
        request.setEmailAddress((String) results.get(0).getOrDefault("email_address", ""));
        request.setPhoto((String) results.get(0).getOrDefault("photo", ""));
        request.setSignature((String) results.get(0).getOrDefault("signature", ""));
        request.setDob((String) results.get(0).getOrDefault("dob", ""));
        request.setMaritalStatus((String) results.get(0).getOrDefault("marital_status", ""));
        request.setBirthRegion((String) results.get(0).getOrDefault("birth_region", ""));
        request.setBirthDistrict((String) results.get(0).getOrDefault("birth_district", ""));
        request.setBirthPlace((String) results.get(0).getOrDefault("birth_place", ""));
        request.setBirthWard((String) results.get(0).getOrDefault("birth_ward", ""));
        request.setResidenceRegion((String) results.get(0).getOrDefault("residence_region", ""));
        request.setResidenceDistrict((String) results.get(0).getOrDefault("residence_district", ""));
        request.setResidenceArea((String) results.get(0).getOrDefault("residence_area", ""));
        request.setResidenceHouseNo((String) results.get(0).getOrDefault("residence_house_no", ""));
        String city = (String) results.get(0).getOrDefault("city", "");
        if (city.isEmpty())
            request.setCity("DAR ES SALAAM");
        else
            request.setCity(city);
        request.setCustomerTypeId("718950");
        request.setCustomerTypeCat("80");
        request.setBranchCode((String) results.get(0).getOrDefault("branch_code", ""));
        request.setBranchId((String) results.get(0).getOrDefault("branch_id", ""));
        request.setBranchName((String) results.get(0).getOrDefault("branch_name", ""));
        request.setMonthlyIncome((String) results.get(0).getOrDefault("monthly_income", ""));
        request.setChequeNumber((String) results.get(0).getOrDefault("cheque_number", ""));
        request.setAmlServiceId((Integer) results.get(0).getOrDefault("aml_service_id", 21));
        request.setDisabilityStatusId((Integer) results.get(0).getOrDefault("disability_status_id", 3000));
        request.setBusinessActivityId((String) results.get(0).getOrDefault("business_activity_id", 21));
        request.setOpeningReasonId((String) results.get(0).getOrDefault("opening_reason_id", "549"));
        request.setIsPE((Boolean) results.get(0).getOrDefault("is_pe", false));

        String response = HttpClientService.sendRIMData(request, url);
        CreateCustomerResponse cbsResp = new Gson().fromJson(response, CreateCustomerResponse.class);

        //update after getting response
        String updateSql = "UPDATE customers SET cbs_response_code = ?, cbs_response_message = ?, cust_no = ?, action = ?, action_status = ?, action_comments = ?";

        if (cbsResp.getResponseCode().equals("0")) {
            results = this.jdbcKycTemplate.queryForList(updateSql, new Object[]{cbsResp.getResponseCode(), cbsResp.getMessage(), cbsResp.getCustomerRim(), "C", "Request completed successfully"});
            try {
                this.jacksonMapper.writeValueAsString(results);
                return "{\"success\":true, \"rim\":\"" + cbsResp.getCustomerRim() + "\", \"account\":\"" + cbsResp.getAccountNo() + "\", \"reference\":\"" + cbsResp.getReference() + "\", \"message\": \"" + cbsResp.getMessage() + "\"}";
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return "{\"success\": false, \"message\": \"Failed to create RIM\"}";
            }
        } else {
            results = this.jdbcKycTemplate.queryForList(updateSql, new Object[]{cbsResp.getResponseCode(), cbsResp.getMessage(), cbsResp.getCustomerRim(), "F", "Failed to create RIM"});
            try {
                this.jacksonMapper.writeValueAsString(results);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return "{\"success\": false, \"message\": \"Failed to create RIM\"}";
        }
    }

    public String createRIM(String url, String userId, String category) {
        int results;
        String response = HttpClientService.approveRIM(url);
        CreateCustomerResponse cbsResp = new Gson().fromJson(response, CreateCustomerResponse.class);

        //update after getting response
        String updateSql = "UPDATE customers SET cbs_response_code = ?, cbs_response_message = ?, cust_no = ?, action = ?, action_status = ?, action_comments = ? WHERE id = ?";

        if (cbsResp.getResponseCode().equals("0")) {
            results = this.jdbcKycTemplate.update(updateSql, new Object[]{cbsResp.getResponseCode(), cbsResp.getMessage(), cbsResp.getCustomerRim(), category, "C", "Request completed successfully", userId});
            try {
                this.jacksonMapper.writeValueAsString(results);
                return "{\"success\":true, \"rim\":\"" + cbsResp.getCustomerRim() + "\", \"account\":\"" + cbsResp.getAccountNo() + "\", \"reference\":\"" + cbsResp.getReference() + "\", \"message\": \"" + cbsResp.getMessage() + "\"}";
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return "{\"success\": false, \"message\": \"Failed to create RIM\"}";
            }
        } else {
            results = this.jdbcKycTemplate.update(updateSql, new Object[]{cbsResp.getResponseCode(), cbsResp.getMessage(), cbsResp.getCustomerRim(), category, "F", "Failed to create RIM", userId});
            try {
                this.jacksonMapper.writeValueAsString(results);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return "{\"success\": false, \"message\": \"Failed to create RIM\"}";
        }
    }

    public String createAccount(String url) {
        String response = HttpClientService.createAccount(null, url);
        CreateAccountResponse resp = new Gson().fromJson(response, CreateAccountResponse.class);
        if (resp.getResponseCode().equals("0")) {
            return "{\"success\":true, \"rim\":\"" + resp.getCustomerRim() + "\", \"account\":\"" + resp.getAccountNo() + "\", \"reference\":\"" + resp.getReference() + "\", \"message\": \"" + resp.getMessage() + "\"}";
        } else {
            return "{\"success\":false}";
        }
    }

    public String enrollMobileBanking(String url) {
        String response = HttpClientService.enrollMobileChannel(null, url);
        AccountResponse resp = new Gson().fromJson(response, AccountResponse.class);
        if (resp.getResponseCode().equals("0")) {
            return "{\"success\":true}";
        } else {
            return "{\"success\":false}";
        }
    }

    public EnrollMobileChannelToAccountRequest constructRequest(String acctNo) {
        EnrollMobileChannelToAccountRequest request = new EnrollMobileChannelToAccountRequest();
        request.setAcctNo(acctNo);
        try {
            String str = "select b.branch_id, a.branch_code, b.name, a.account_no, a.customer_number, a.phone_number " +
                    "from accounts a inner join branches b on a.branch_code = b.code where a.account_no = ?";
            Map<String, Object> map = jdbcKycTemplate.queryForMap(str, acctNo);
            request.setBranchCode((String) map.get("branch_code"));
            request.setBranchId((String) map.get("branch_id"));
            request.setBranchName((String) map.get("name"));
            request.setAccessCode((String) map.get("phone_number"));
            request.setCustomerNumber((String) map.get("customer_number"));
        } catch (Exception exception) {
            LOGGER.info(null, exception);
            return null;
        }

        return request;
    }

    public String resetMobileChannelUser(String url, String acctNo) {
        EnrollMobileChannelToAccountRequest enrollMobileChannelToAccountRequest = constructRequest(acctNo);
        if (enrollMobileChannelToAccountRequest != null) {
            String request = new Gson().toJson(enrollMobileChannelToAccountRequest, EnrollMobileChannelToAccountRequest.class);
            String response = HttpClientService.sendJsonRequest(request, url);
            AccountResponse resp = new Gson().fromJson(response, AccountResponse.class);
            if (resp.getResponseCode().equals("0")) {
                return "{\"success\":true}";
            } else {
                return "{\"success\":false}";
            }
        } else {
            return "{\"success\":false}";
        }
    }

    public Map<String, Boolean> hasRIMAndIsEnrolled(String userId) {
        boolean hasRim = false;
        boolean isEnrolledMobileChannel = false;
        try {
            String sql = "SELECT top 1 customers.cust_no as customer_no, connect_to_mobile_channel FROM accounts INNER JOIN customers on accounts.customer_number=customers.cust_no where customers.id = ? order by accounts.id desc";
            Map<String, Object> result = jdbcKycTemplate.queryForMap(sql, new Object[]{userId});
            String rimNo = (String) result.getOrDefault("customer_no", "");
            String isEnrolled = (String) result.getOrDefault("connect_to_mobile_channel", "");
            if (rimNo.length() == 10) hasRim = true;
            if (isEnrolled.equals("Yes")) isEnrolledMobileChannel = true;
            Map<String, Boolean> map = new HashMap<>();
            map.put("hasRim", hasRim);
            map.put("isEnrolled", isEnrolledMobileChannel);
            return map;
        } catch (Exception ex) {
            LOGGER.info("Has RIM: ", ex);
            return null;
        }
    }

    public Boolean hasAccount(String userId) {
        boolean hasAccount = false;
        try {
            String countSql = "SELECT count(accounts.id) as count FROM accounts INNER JOIN customers on accounts.customer_number=customers.cust_no where customers.id = ? and accounts.cbs_response_code = 0";
            Integer totalRecords = jdbcKycTemplate.queryForObject(countSql, new Object[]{userId}, Integer.class);
            if (totalRecords != null && totalRecords > 0) hasAccount = true;
            return hasAccount;
        } catch (Exception ex) {
            LOGGER.info("Has Account: ", ex);
            return null;
        }
    }

    public String getCustomerInfo(String customerNo) throws JsonProcessingException {
        QueryCustomerResponse queryCustomerResponse = new QueryCustomerResponse();
        String request = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:api=\"http://api.PHilae/\">" +
                "   <soapenv:Header/>" +
                "   <soapenv:Body>" +
                "      <api:queryCustomer>" +
                "         <!--Optional:-->" +
                "         <request>" +
                "            <reference>"+System.currentTimeMillis()+"</reference>" +
                "            <!--Optional:-->" +
                "            <custId>?</custId>" +
                "            <!--Optional:-->" +
                "            <custNo>"+customerNo+"</custNo>" +
                "         </request>" +
                "      </api:queryCustomer>" +
                "   </soapenv:Body>" +
                "</soapenv:Envelope>";
//        request = request.replace(" ", "");
        //send http request to wsdl server
        String soapResponse = HttpClientService.sendXMLReqBasicAuth(request, sysenv.CHANNEL_MANAGER_API_URL, "xapi", "x@pi#81*");
        if (soapResponse.contains("return")) {
            //parser soap xml to get clean xml
            XMLStreamReader xmlr = XMLParserService.createXMLStreamReaderFromSOAPMessage(soapResponse, "body", "return");
            //byte data to string xml
            String sxml = XMLParserService.xmlsrToString(xmlr);
            LOGGER.info("Final response xml... {}", sxml);
            //convert xml to java object
            queryCustomerResponse.setResult(XMLParserService.getDomTagText("result", sxml));
            queryCustomerResponse.setMessage(XMLParserService.getDomTagText("message", sxml));
            queryCustomerResponse.setCustName(XMLParserService.getDomTagText("custName", sxml));
            queryCustomerResponse.setDob(XMLParserService.getDomTagText("dateOfBirth", sxml));
            queryCustomerResponse.setGender(XMLParserService.getDomTagText("gender", sxml));
            queryCustomerResponse.setCustNo(XMLParserService.getDomTagText("custNo", sxml));
            queryCustomerResponse.setCustCat(XMLParserService.getDomTagText("custCat", sxml));
            queryCustomerResponse.setCustId(XMLParserService.getDomTagText("custId", sxml));
            queryCustomerResponse.setPhoto(XMLParserService.getDomTagText("photo", sxml));
            queryCustomerResponse.setSignature(XMLParserService.getDomTagText("signature", sxml));
            queryCustomerResponse.setMobileNumber(XMLParserService.getDomTagText("mobileNumber", sxml));
            queryCustomerResponse.setAccountName(XMLParserService.getDomTagText("accountName", sxml));
            queryCustomerResponse.setAccountNumber(XMLParserService.getDomTagText("accountNumber", sxml));
            queryCustomerResponse.setAccountType(XMLParserService.getDomTagText("accountType", sxml));
            queryCustomerResponse.setAcctId(XMLParserService.getDomTagText("acctId", sxml));
            queryCustomerResponse.setStatus(XMLParserService.getDomTagText("status", sxml));
            queryCustomerResponse.setBuId(XMLParserService.getDomTagText("buId", sxml));
            queryCustomerResponse.setBuCode(XMLParserService.getDomTagText("buCode", sxml));
            queryCustomerResponse.setBuName(XMLParserService.getDomTagText("buName", sxml));
            queryCustomerResponse.setGlPrefix(XMLParserService.getDomTagText("glPrefix", sxml));
            queryCustomerResponse.setId(XMLParserService.getDomTagText("identity", sxml));
            queryCustomerResponse.setCode(XMLParserService.getDomTagText("code", sxml));
            queryCustomerResponse.setName(XMLParserService.getDomTagText("name", sxml));
            queryCustomerResponse.setProductId(XMLParserService.getDomTagText("productId", sxml));
        }
        try {
            return jacksonMapper.writeValueAsString(queryCustomerResponse);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public int updateAgentDevice(String userId) {
        int lastUpdate = -1;
        try {
            String countSql = "UPDATE users SET first_login = 'Y' where id = ?";
            lastUpdate = jdbcKycTemplate.update(countSql, userId);
            return lastUpdate;
        } catch (Exception ex) {
            ex.printStackTrace();
            return lastUpdate;
        }
    }

    public int updateAgentBranch(String userId, String branchCode) {
        LOGGER.info("ID: {}, BRANCH CODE: {}", userId, branchCode);
        int lastUpdate = -1;
        try {
            String countSql = "UPDATE users SET branch_code = ? where id = ?";
            lastUpdate = jdbcKycTemplate.update(countSql, branchCode, userId);
            return lastUpdate;
        } catch (Exception ex) {
            ex.printStackTrace();
            return lastUpdate;
        }
    }

    public String getBranchesReport() {
        try {
            String str = "select distinct b.name, count(a.id) count from accounts a inner join branches b on a.branch_code = b.code where cbs_response_code = 0 group by b.name";
            return jacksonMapper.writeValueAsString(jdbcKycTemplate.queryForList(str));
        } catch (Exception exception) {
            LOGGER.info(null, exception);
            return null;
        }
    }

    public List<Map<String, Object>> getPendingBranches() {
        try {
            String str = "select name from branches where code not in (select branch_code from accounts)";
            return jdbcKycTemplate.queryForList(str);
        } catch (Exception exception) {
            LOGGER.info(null, exception);
            return null;
        }
    }

    public List<Map<String, Object>> getThisYearSuccessAccounts() {
        LocalDate now = LocalDate.now();
        LocalDate firstDay = now.with(firstDayOfYear());
        try {
            String str = "select a.*, b.name from accounts a inner join branches b on b.code = a.branch_code where " +
                    "a.created_date > ? and cbs_response_code = '0' order by b.name";
            return jdbcKycTemplate.queryForList(str, firstDay);
        } catch (Exception exception) {
            LOGGER.info(null, exception);
            return null;
        }
    }

    public Integer getThisYearSuccessNIDAAccountsCount() {
        LocalDate now = LocalDate.now();
        LocalDate firstDay = now.with(firstDayOfYear());
        try {
            String str = "select count(*) from accounts where customer_number in (SELECT x.cust_no FROM customers x " +
                    "WHERE [action] = 'NIDA' and cbs_response_code = '0') and created_date > ? and cbs_response_code = '0'";
            return jdbcKycTemplate.queryForObject(str, Integer.class, firstDay);
        } catch (Exception exception) {
            LOGGER.info(null, exception);
            return null;
        }
    }

    public Integer getThisYearSuccessNonNIDAAccountsCount() {
        LocalDate now = LocalDate.now();
        LocalDate firstDay = now.with(firstDayOfYear());
        try {
            String str = "select count(*) from accounts where customer_number in (SELECT x.cust_no FROM customers x " +
                    "WHERE [action] = 'ATTACHMENT' and cbs_response_code = '0') and created_date > ? and cbs_response_code = '0'";
            return jdbcKycTemplate.queryForObject(str, Integer.class, firstDay);
        } catch (Exception exception) {
            LOGGER.info(null, exception);
            return null;
        }
    }

    public Integer getThisYearSuccessAccountsCount() {
        LocalDate now = LocalDate.now();
        LocalDate firstDay = now.with(firstDayOfYear());
        try {
            String str = "select count(*) from accounts where created_date > ? and cbs_response_code = '0'";
            return jdbcKycTemplate.queryForObject(str, Integer.class, firstDay);
        } catch (Exception exception) {
            LOGGER.info(null, exception);
            return null;
        }
    }

    public String remoteAccountOpeningReport(String exporterFileType, HttpServletResponse response, String destName,
                                             String reportType, String fromDate, String toDate, String username,
                                             String branch, Integer isAgent, String agent) {
        Connection conn = null;
        String reportFileTemplate = "/iReports/kyc/RAO_report.jasper";
        if (reportType.equals("Categorized")) {
            reportFileTemplate = "/iReports/kyc/kyc_report.jasper";
        }
        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("START_DATE", fromDate + " 00:00:00");
            parameters.put("END_DATE", toDate + " 23:59:59");
            parameters.put("PRINTED_BY", username);
            parameters.put("PRINTED_AT", DateUtil.now("dd MMM yyyy"));
            if (isAgent != null && isAgent == 1) {
                parameters.put("WAKALA", true);
            } else {
                parameters.put("WAKALA", false);
            }
            if (branch != null && !branch.isEmpty()) {
                reportFileTemplate = "/iReports/kyc/RAO_branch_report.jasper";
                parameters.put("BRANCH_CODE", branch);
            }
            if (agent != null && !agent.isEmpty()) {
                reportFileTemplate = "/iReports/kyc/RAO_agent_report.jasper";
                parameters.put("AGENT", agent);
            }
            if (isAgent != null && (isAgent == 0 || isAgent == 1)) {
                reportFileTemplate = "/iReports/kyc/RAO_wakala_report.jasper";
            }
            if (isAgent != null && isAgent == 2) {
                reportFileTemplate = "/iReports/kyc/RAO_vendor_report.jasper";
                parameters.put("CATEGORY", "MAINSTREAM");
            }
            conn = kycDatasource.getConnection();
            JasperPrint print = jasperService.jasperPrint(reportFileTemplate, parameters, conn);
            conn.close();
            return jasperService.exportFileOption(print, exporterFileType, response, destName);
        } catch (Exception ex) {
            Logger.getLogger(KycRepo.class.getName()).log(Level.SEVERE, "Error", ex);
            return null;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String getSuccessAccountsReport(String exporterFileType, HttpServletResponse response, String destName,
                                           String username) {
        String reportFileTemplate = "/iReports/kyc/success_accounts_report.jasper";
        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("PRINTED_BY", username);
            parameters.put("PRINTED_AT", DateUtil.now("dd MMM yyyy"));
            JasperPrint print = jasperService.jasperPrint(reportFileTemplate, parameters, new JRBeanCollectionDataSource(getThisYearSuccessAccounts()));
            return jasperService.exportFileOption(print, exporterFileType, response, destName);
        } catch (Exception ex) {
            Logger.getLogger(KycRepo.class.getName()).log(Level.SEVERE, "Error", ex);
            return null;
        }
    }

    public String getAccountsWorkflows(String branchCode, String branch, String status, String fromDate, String toDate,
                                       String draw, String start, String rowPerPage,
                                       String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results;
        String mainSql;
        int totalRecordWithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        if (Objects.equals(branchCode, "060") || Objects.equals(branchCode, "500") || Objects.equals(branchCode, "170") || Objects.equals(branchCode, "420"))
            branchCode = null;
        try {
            if (branchCode == null || branchCode.isEmpty()) {
                mainSql = "SELECT count(id) FROM mofp_workflows where status LIKE ?";
                totalRecords = jdbcTemplate.queryForObject(mainSql, Integer.class, "%" + status + "%");
            } else {
                mainSql = "SELECT count(id) FROM mofp_workflows where branch_code=? and status LIKE ?";
                totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{branchCode, "%" + status + "%"}, Integer.class);
            }
            String searchQuery;
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                if (branchCode == null || branchCode.isEmpty()) {
                    searchQuery = " WHERE concat(created_by,' ',created_date,' ',last_modified_by,' ',last_modified_date,' ',rec_status,' ',sender,' ',receiver,' ',msg_id,' ',payment_type,' ',message_type,' ',bic,' ',authority_ref,' ',bot_ref,' ',branch_code) LIKE ? and status LIKE ?";
                    totalRecordWithFilter = jdbcKycTemplate.queryForObject("SELECT count(id) FROM mofp_workflows" + searchQuery, new Object[]{searchValue, status}, Integer.class);
                    mainSql = "SELECT ma.*, b.name branch, GROUP_CONCAT(mwa.id ORDER BY mwa.id) attachments FROM mofp_workflows ma INNER JOIN branches b on ma.branch_code = b.code LEFT JOIN mofp_wf_attachments mwa ON ma.id = mwa.mofp_workflows_id" + searchQuery + "GROUP BY ma.id ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                    results = jdbcTemplate.queryForList(mainSql, searchValue, "%" + status + "%");
                } else {
                    searchQuery = " WHERE concat(created_by,' ',created_date,' ',last_modified_by,' ',last_modified_date,' ',rec_status,' ',sender,' ',receiver,' ',msg_id,' ',payment_type,' ',message_type,' ',bic,' ',authority_ref,' ',bot_ref) LIKE ? and branch_code=? and status LIKE ?";
                    totalRecordWithFilter = jdbcKycTemplate.queryForObject("SELECT count(id) FROM mofp_workflows" + searchQuery, new Object[]{searchValue, branchCode, status}, Integer.class);
                    mainSql = "SELECT ma.*, b.name branch, GROUP_CONCAT(mwa.id ORDER BY mwa.id) attachments FROM mofp_workflows ma INNER JOIN branches b on ma.branch_code = b.code LEFT JOIN mofp_wf_attachments mwa ON ma.id = mwa.mofp_workflows_id" + searchQuery + "GROUP BY ma.id ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                    results = jdbcTemplate.queryForList(mainSql, searchValue, branchCode, "%" + status + "%");
                }
            } else {
                totalRecordWithFilter = totalRecords;
                if (branchCode == null || branchCode.isEmpty()) {
                    mainSql = "SELECT ma.*, b.name branch, GROUP_CONCAT(mwa.id ORDER BY mwa.id) attachments FROM mofp_workflows ma INNER JOIN branches b on ma.branch_code = b.code LEFT JOIN mofp_wf_attachments mwa ON ma.id = mwa.mofp_workflows_id where status LIKE ? GROUP BY ma.id ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                    results = jdbcTemplate.queryForList(mainSql, "%" + status + "%");
                } else {
                    mainSql = "SELECT ma.*, b.name branch, GROUP_CONCAT(mwa.id ORDER BY mwa.id) attachments FROM mofp_workflows ma INNER JOIN branches b on ma.branch_code = b.code LEFT JOIN mofp_wf_attachments mwa ON ma.id = mwa.mofp_workflows_id where branch_code=? and status LIKE ? GROUP BY ma.id ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                    results = jdbcTemplate.queryForList(mainSql, branchCode, "%" + status + "%");
                }
            }
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        return "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordWithFilter + "\",\"aaData\":" + jsonString + "}";
    }

    public String getWorkflowAccounts(String branchCode, String workflowId, String draw, String start, String rowPerPage,
                                      String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results;
        String mainSql;
        int totalRecordWithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        if (Objects.equals(branchCode, "060") || Objects.equals(branchCode, "500") || Objects.equals(branchCode, "170") || Objects.equals(branchCode, "420"))
            branchCode = null;
        try {
            if (branchCode == null || branchCode.isEmpty()) {
                mainSql = "SELECT count(id) FROM mofp_wf_accounts where mofp_workflows_id=?";
                totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{workflowId}, Integer.class);
            } else {
                mainSql = "SELECT count(id) FROM mofp_wf_accounts where substring(branchCode, 4)=? and mofp_workflows_id=?";
                totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{branchCode, workflowId}, Integer.class);
            }
            String searchQuery;
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                if (branchCode == null || branchCode.isEmpty()) {
                    searchQuery = " WHERE concat(created_by,' ',created_date,' ',last_modified_by,' ',last_modified_date,' ',rec_status,' ',endtoEndId,' ',account_no,' ',account_title,' ',ccy,' ',transBic,' ',transAcctNum,' ',transAcctName,' ',transCcy,' ',regionCode,' ',district,' ',phoneNum,' ',email,' ',postalAddr,' ',category,' ',acct_type,' ',branchCode,' ',operator,' ',owner,' ',operatorCat,' ',purpose,' ',status_desc,' ',code) LIKE ? and mofp_workflows_id=?";
                    totalRecordWithFilter = jdbcKycTemplate.queryForObject("SELECT count(id) FROM mofp_wf_accounts " + searchQuery, new Object[]{searchValue, workflowId}, Integer.class);
                    mainSql = "SELECT ma.*, b.name branch FROM mofp_wf_accounts ma INNER JOIN branches b on substring(ma.branchCode, 4) = b.code" + searchQuery + "ORDER BY " + columnName + " " + columnSortOrder  + " limit " + start + "," + rowPerPage;
                    results = jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, workflowId});
                } else {
                    searchQuery = " WHERE concat(created_by,' ',created_date,' ',last_modified_by,' ',last_modified_date,' ',rec_status,' ',endtoEndId,' ',account_no,' ',account_title,' ',ccy,' ',transBic,' ',transAcctNum,' ',transAcctName,' ',transCcy,' ',regionCode,' ',district,' ',phoneNum,' ',email,' ',postalAddr,' ',category,' ',acct_type,' ',branchCode,' ',operator,' ',owner,' ',operatorCat,' ',purpose,' ',status_desc) LIKE ? and code=? and mofp_workflows_id=?";
                    totalRecordWithFilter = jdbcKycTemplate.queryForObject("SELECT count(id) FROM mofp_wf_accounts " + searchQuery, new Object[]{searchValue, branchCode, workflowId}, Integer.class);
                    mainSql = "SELECT ma.*, b.name branch FROM mofp_wf_accounts ma INNER JOIN branches b on substring(ma.branchCode, 4) = b.code" + searchQuery + "ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                    results = jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, branchCode, workflowId});
                }
            } else {
                totalRecordWithFilter = totalRecords;
                if (branchCode == null || branchCode.isEmpty()) {
                    mainSql = "SELECT ma.*, b.name branch FROM mofp_wf_accounts ma INNER JOIN branches b on substring(ma.branchCode, 4) = b.code where mofp_workflows_id=? ORDER BY " + columnName + " " + columnSortOrder  + " limit " + start + "," + rowPerPage;
                    results = jdbcTemplate.queryForList(mainSql, workflowId);
                } else {
                    mainSql = "SELECT ma.*, b.name branch FROM mofp_wf_accounts ma INNER JOIN branches b on substring(ma.branchCode, 4) = b.code where substring(ma.branchCode, 4)=? and mofp_workflows_id=? ORDER BY " + columnName + " " + columnSortOrder  + " limit " + start + "," + rowPerPage;
                    results = jdbcTemplate.queryForList(mainSql, new Object[]{branchCode, workflowId});
                }
            }
            jsonString = jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        return "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordWithFilter + "\",\"aaData\":" + jsonString + "}";

    }

    public String getAttachmentDocument(String ref) {
        String result;
        try {
            String sql = "select attachment from mofp_wf_attachments where mofp_workflows_id = ? limit 1";
            result = jdbcTemplate.queryForObject(sql, new Object[]{ref}, (rs, rowNum) -> rs.getString(1));
            LOGGER.info("Result: {}", result);
        } catch (DataAccessException e) {
            return null;
        }
        return result;
    }

    public byte[] downloadStatusDoc(String ref) {
        byte[] result;
        try {
            String sql = "select attachment from mofp_wf_attachments where id = ?";
            String resp = jdbcTemplate.queryForObject(sql, new Object[]{ref}, (rs, rowNum) -> rs.getString(1));
            String cleanResp = resp.replace("\n", "");
            result = Base64.decodeBase64(cleanResp);
        } catch (DataAccessException e) {
            return null;
        }
        return result;
    }

    public String openMoFPAccountRequest(String payload) {
        int mofpWorkflowsId;
        try {
            Document document = XMLParserService.jaxbXMLToObject(payload, Document.class);
            LOGGER.info("Document {}", document);
            Header header = document.getHeader();
            LOGGER.info("Header {}", header);
            MsgSummary msgSummary = document.getMsgSummary();
            LOGGER.info("MsgSummary {}", msgSummary);
            try {
                String mainSql = "SELECT * FROM mofp_workflows WHERE msg_id = ?";
                List<Map<String, Object>> results = jdbcTemplate.queryForList(mainSql, header.getMsgId());
                LOGGER.info("Results {}", results);
                String respStatus = "ACCEPTED";
                String description = "Message has been Received and Saved";
                if (!results.isEmpty()) {
                    respStatus = "REJECTED";
                    description = "Duplicate request";
                }

                KeyHolder keyHolder = new GeneratedKeyHolder();
                String sql = "INSERT INTO mofp_workflows (created_by, created_date, rec_status, sender, receiver, msg_id," +
                        " payment_type, message_type, bic, authority_ref, bot_ref, create_dt, expires_dt, branch_code," +
                        " status, nb_of_rec, resp_status, description) VALUES ('SYSTEM', ?, 'A', ?, ?, ?, ?, ?, ?, ?, ?," +
                        " ?, ?, ?, 'Pending Open', ?, ?, ?)";
                String finalSql = sql;
                String finalRespStatus = respStatus;
                String finalDescription = description;
                this.jdbcTemplate.update(connection -> {
                    PreparedStatement ps = connection.prepareStatement(finalSql, new String[] {"id"});
                    ps.setString(1, DateUtil.now());
                    ps.setString(2, header.getSender());
                    ps.setString(3, header.getReceiver());
                    ps.setString(4, header.getMsgId());
                    ps.setString(5, header.getPaymentType());
                    ps.setString(6, header.getMessageType());
                    ps.setString(7, msgSummary.getBic());
                    ps.setString(8, msgSummary.getAuthorityRef());
                    ps.setString(9, msgSummary.getBotRef());
                    ps.setDate(10, new java.sql.Date(msgSummary.getCreDtTm().getTime()));
                    ps.setDate(11, new java.sql.Date(msgSummary.getExprDt().getTime()));
                    ps.setString(12, "060");
                    ps.setInt(13, msgSummary.getNbOfRec());
                    ps.setString(14, finalRespStatus);
                    ps.setString(15, finalDescription);
                    return ps;
                }, keyHolder);
                mofpWorkflowsId = keyHolder.getKey().intValue();
                sql = "INSERT INTO mofp_msg_head_summary (sender, receiver, msgId, paymentType, messageType, bic," +
                        " authorityRef, botRef, creDtTm, exprDt, nbOfRec) VALUES (?, ?, ?, ?, ? ,?, ?, ?, ?, ?, ?)";
                String finalSql2 = sql;
                int mofpMsgHeadSummaryId;
                this.jdbcTemplate.update(connection -> {
                    PreparedStatement ps = connection.prepareStatement(finalSql2, new String[] {"id"});
                    ps.setString(1, header.getSender());
                    ps.setString(2, header.getReceiver());
                    ps.setString(3, header.getMsgId());
                    ps.setString(4, header.getPaymentType());
                    ps.setString(5, header.getMessageType());
                    ps.setString(6, msgSummary.getBic());
                    ps.setString(7, msgSummary.getAuthorityRef());
                    ps.setString(8, msgSummary.getBotRef());
                    ps.setDate(9,  new java.sql.Date(msgSummary.getCreDtTm().getTime()));
                    ps.setDate(10, new java.sql.Date(msgSummary.getExprDt().getTime()));
                    ps.setInt(11, msgSummary.getNbOfRec());
                    return ps;
                }, keyHolder);
                mofpMsgHeadSummaryId = keyHolder.getKey().intValue();
                Details details = document.getDetails();
                LOGGER.info("Details {}", details);
                List<AcctRecord> acctRecordList = details.getAcctRecord();
                String name = "";
                String postalAddress = "";
                for (AcctRecord acctRecord: acctRecordList) {
                    name = acctRecord.getName();
                    postalAddress = acctRecord.getPostalAddr();
                    sql = "INSERT INTO mofp_messages (endToEndId, orgEndToEndId, name, ccy, category, acctType, branchCode, operator," +
                            " owner, operatorCat, purpose, regionCode, district, phoneNum, email, postalAddr," +
                            " msg_head_summary_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    this.jdbcTemplate.update(sql, new Object[]{acctRecord.getEndToEndId(), acctRecord.getEndToEndId(),
                            acctRecord.getName(), acctRecord.getCcy(), acctRecord.getCategory(), acctRecord.getAcctType(),
                            acctRecord.getBranchCode(), acctRecord.getOperator(), acctRecord.getOwner(),
                            acctRecord.getOperatorCat(), acctRecord.getPurpose(), acctRecord.getRegionCode(),
                            acctRecord.getDistrict(), acctRecord.getPhoneNum(), acctRecord.getEmail(),
                            acctRecord.getPostalAddr(), mofpMsgHeadSummaryId
                    });
                    // TESTING
//                    try {
//                        String sql1 = "UPDATE mofp_messages set acctNum = ?, openDt = ?, openingStatus = ?, openingStatusDesc = ? " +
//                                "where endToEndId = ?";
//                        jdbcTemplate.update(sql1, new Object[]{generateRandomAccount(), DateUtil.now(), "Opened",
//                                "Opened successfully", acctRecord.getEndToEndId()});
//                    } catch (DataAccessException e) {
//                        e.printStackTrace();
//                    }
                    try {
                        sql = "INSERT INTO mofp_wf_accounts (created_by, created_date, rec_status, endtoEndId, account_title," +
                                " ccy, regionCode, district, phoneNum, email, postalAddr, mofp_workflows_id, category, acct_type," +
                                " branchCode, operator, owner, operatorCat, purpose, status) VALUES ('SYSTEM', ?, 'A', ?, ?, ?, ?," +
                                " ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'Pending')";
                        this.jdbcTemplate.update(sql, new Object[]{DateUtil.now(), acctRecord.getEndToEndId(), acctRecord.getName(),
                                acctRecord.getCcy(), acctRecord.getRegionCode(), acctRecord.getDistrict(), acctRecord.getPhoneNum(),
                                acctRecord.getEmail(), acctRecord.getPostalAddr(), mofpWorkflowsId, acctRecord.getCategory(), acctRecord.getAcctType(),
                                acctRecord.getBranchCode(), acctRecord.getOperator(), acctRecord.getOwner(), acctRecord.getOperatorCat(),
                                acctRecord.getPurpose()
                        });
                    } catch (DataAccessException e) {
                        e.printStackTrace();
                    }
                }

                Attachments attachments = document.getAttachments();
                if (attachments.getAttachment1() != null && !attachments.getAttachment1().isEmpty()) {
                    try {
                        sql = "INSERT INTO mofp_wf_attachments (created_by, created_date, rec_status, attachment," +
                                " mofp_workflows_id) VALUES ('SYSTEM', ?, 'A', ?, ?)";
                        jdbcTemplate.update(sql, new Object[]{DateUtil.now(), attachments.getAttachment1(), mofpWorkflowsId});
                    } catch (DataAccessException e) {
                        e.printStackTrace();
                    }
                }
                if (attachments.getAttachment2() != null && !attachments.getAttachment2().isEmpty()) {
                    try {
                        sql = "INSERT INTO mofp_wf_attachments (created_by, created_date, rec_status, attachment," +
                                " mofp_workflows_id) VALUES ('SYSTEM', ?, 'A', ?, ?)";
                        jdbcTemplate.update(sql, new Object[]{DateUtil.now(), attachments.getAttachment2(), mofpWorkflowsId});
                    } catch (DataAccessException e) {
                        e.printStackTrace();
                    }
                }
                ExecutorService executor = Executors.newFixedThreadPool(1);
                String finalName = name;
                String finalPostalAddress = postalAddress;
                executor.submit(() -> {
                    try {
                        Thread.sleep(2000);
                        sendBotMoFPResponse(1, String.valueOf(mofpWorkflowsId), "Message has been Received and Saved");
//                        Thread.sleep(2000);
//                        String tcbReference = "TCB" + DateUtil.now("yyyyMMddHHmm");
//                        sendMoFPAccountOpeningStatus((long) mofpMsgHeadSummaryId, tcbReference, finalName, finalPostalAddress);
//                        Thread.sleep(2000);
//                        sendBotMoFPResponse(2, String.valueOf(mofpWorkflowsId), "Message has been processed");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                executor.shutdown();
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("Threads didn't finish in 1 minute!");
                }
            } catch(DataAccessException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "OK";
    }

    public String maintainMoFPAccountRequest(String payload) {
        int mofpWorkflowsId;
        try {
            Document document = XMLParserService.jaxbXMLToObject(payload, Document.class);
            LOGGER.info("Document {}", document);
            Header header = document.getHeader();
            LOGGER.info("Header {}", header);
            MsgSummary msgSummary = document.getMsgSummary();
            LOGGER.info("MsgSummary {}", msgSummary);
            try {
                String mainSql = "SELECT * FROM mofp_workflows WHERE msg_id = ?";
                List<Map<String, Object>> results = jdbcTemplate.queryForList(mainSql, header.getMsgId());
                String respStatus = "ACCEPTED";
                String description = "Message has been Received and Saved";
                if (!results.isEmpty()) {
                    respStatus = "REJECTED";
                    description = "Duplicate request";
                }

                KeyHolder keyHolder = new GeneratedKeyHolder();
                String sql = "INSERT INTO mofp_workflows (created_by, created_date, rec_status, sender, receiver, msg_id," +
                        " payment_type, message_type, bic, authority_ref, bot_ref, create_dt, expires_dt, branch_code," +
                        " status, nb_of_rec, resp_status, description) VALUES ('SYSTEM', ?, 'A', ?, ?, ?, ?, ?, ?, ?, ?," +
                        " ?, ?, ?, 'Pending Change', ?, ?, ?)";
                String finalSql = sql;
                String finalRespStatus = respStatus;
                String finalDescription = description;
                this.jdbcTemplate.update(connection -> {
                    PreparedStatement ps = connection.prepareStatement(finalSql, new String[] {"id"});
                    ps.setString(1, DateUtil.now());
                    ps.setString(2, header.getSender());
                    ps.setString(3, header.getReceiver());
                    ps.setString(4, header.getMsgId());
                    ps.setString(5, header.getPaymentType());
                    ps.setString(6, header.getMessageType());
                    ps.setString(7, msgSummary.getBic());
                    ps.setString(8, msgSummary.getAuthorityRef());
                    ps.setString(9, msgSummary.getBotRef());
                    ps.setDate(10, new java.sql.Date(msgSummary.getCreDtTm().getTime()));
                    ps.setDate(11, new java.sql.Date(msgSummary.getExprDt().getTime()));
                    ps.setString(12, "060");
                    ps.setInt(13, msgSummary.getNbOfRec());
                    ps.setString(14, finalRespStatus);
                    ps.setString(15, finalDescription);
                    return ps;
                }, keyHolder);
                mofpWorkflowsId = keyHolder.getKey().intValue();

                sql = "INSERT INTO mofp_msg_head_summary (sender, receiver, msgId, paymentType, messageType, bic," +
                        " authorityRef, botRef, creDtTm, exprDt, nbOfRec) VALUES (?, ?, ?, ?, ? ,?, ?, ?, ?, ?, ?)";
                String finalSql2 = sql;
                int mofpMsgHeadSummaryId;
                this.jdbcTemplate.update(connection -> {
                    PreparedStatement ps = connection.prepareStatement(finalSql2, new String[] {"id"});
                    ps.setString(1, header.getSender());
                    ps.setString(2, header.getReceiver());
                    ps.setString(3, header.getMsgId());
                    ps.setString(4, header.getPaymentType());
                    ps.setString(5, header.getMessageType());
                    ps.setString(6, msgSummary.getBic());
                    ps.setString(7, msgSummary.getAuthorityRef());
                    ps.setString(8, msgSummary.getBotRef());
                    ps.setDate(9,  new java.sql.Date(msgSummary.getCreDtTm().getTime()));
                    ps.setDate(10, new java.sql.Date(msgSummary.getExprDt().getTime()));
                    ps.setInt(11, msgSummary.getNbOfRec());
                    return ps;
                }, keyHolder);
                mofpMsgHeadSummaryId = keyHolder.getKey().intValue();
                Details details = document.getDetails();
                LOGGER.info("Details {}", details);
                List<AcctRecord> acctRecordList = details.getAcctRecord();
                String name = "";
                String postalAddress = "";
                for (AcctRecord acctRecord: acctRecordList) {
                    name = acctRecord.getName();
                    postalAddress = acctRecord.getPostalAddr();
                    String branchCode = this.jdbcTemplate.queryForObject("select branchCode from mofp_wf_accounts where account_no = ?",
                            new Object[] {acctRecord.getAcctNum()}, String.class);
                    if (Objects.equals(branchCode, "-1")) {
                        branchCode = "048170";
                    }
                    sql = "INSERT INTO mofp_messages (endToEndId, acctNum, branchCode, name, oldName, newName, ccy, otherDetails," +
                            " regionCode, district, phoneNum, email, postalAddr, msg_head_summary_id) VALUES (" +
                            "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    this.jdbcTemplate.update(sql, new Object[]{acctRecord.getEndToEndId(), acctRecord.getAcctNum(),
                            branchCode, acctRecord.getName(), acctRecord.getName(), acctRecord.getNewName(), acctRecord.getCcy(),
                            acctRecord.getOtherDetails(), acctRecord.getRegionCode(), acctRecord.getDistrict(),
                            acctRecord.getPhoneNum(), acctRecord.getEmail(), acctRecord.getPostalAddr(), mofpMsgHeadSummaryId
                    });
                    // TESTING
                    if (Objects.equals(acctRecord.getOtherDetails(), "SAME")) {
                        try {
                            String sql1 = "UPDATE mofp_messages set oldName = ?, newName = ?, changeDtTm = ?, " +
                                    "changingStatus = ?, changingStatusDesc = ? where endToEndId = ?";
                            jdbcTemplate.update(sql1, new Object[]{acctRecord.getName(), acctRecord.getNewName(),
                                    DateUtil.now(), "Changed", "Changed successfully", acctRecord.getEndToEndId()});
                        } catch (DataAccessException e) {
                            e.printStackTrace();
                        }
                    } else if (Objects.equals(acctRecord.getOtherDetails(), "CHANGE")) {
                        try {
                            String sql1 = "UPDATE mofp_messages set oldName = ?, newName = ?, changeDtTm = ?, " +
                                    "changingStatus = ?, changingStatusDesc = ?, regionCode = ?, district = ?, " +
                                    "phoneNum = ?, email = ?, postalAddr = ? where endToEndId = ?";
                            jdbcTemplate.update(sql1, new Object[]{acctRecord.getName(), acctRecord.getNewName(),
                                    DateUtil.now(), "Changed", "Changed successfully", acctRecord.getRegionCode(),
                                    acctRecord.getDistrict(), acctRecord.getPhoneNum(), acctRecord.getEmail(),
                                    acctRecord.getPostalAddr(), acctRecord.getEndToEndId()});
                        } catch (DataAccessException e) {
                            e.printStackTrace();
                        }
                    } else if (Objects.equals(acctRecord.getOtherDetails(), "ACTIVATE")) {
                        //TODO Activate account from Dormant to Active
                    }
                    try {
                        sql = "UPDATE mofp_wf_accounts SET status = 'Pending Change', account_title = ? WHERE account_no = ?";
                        this.jdbcTemplate.update(sql, acctRecord.getNewName(), acctRecord.getAcctNum());
                    } catch (DataAccessException e) {
                        e.printStackTrace();
                    }
                }

                Attachments attachments = document.getAttachments();
                if (attachments.getAttachment1() != null && !attachments.getAttachment1().isEmpty()) {
                    try {
                        sql = "INSERT INTO mofp_wf_attachments (created_by, created_date, rec_status, attachment," +
                                " mofp_workflows_id) VALUES ('SYSTEM', ?, 'A', ?, ?)";
                        jdbcTemplate.update(sql, new Object[]{DateUtil.now(), attachments.getAttachment1(), mofpWorkflowsId});
                    } catch (DataAccessException e) {
                        e.printStackTrace();
                    }
                }
                if (attachments.getAttachment2() != null && !attachments.getAttachment2().isEmpty()) {
                    try {
                        sql = "INSERT INTO mofp_wf_attachments (created_by, created_date, rec_status, attachment," +
                                " mofp_workflows_id) VALUES ('SYSTEM', ?, 'A', ?, ?)";
                        jdbcTemplate.update(sql, new Object[]{DateUtil.now(), attachments.getAttachment2(), mofpWorkflowsId});
                    } catch (DataAccessException e) {
                        e.printStackTrace();
                    }
                }
                ExecutorService executor = Executors.newFixedThreadPool(1);
                String finalName = name;
                String finalPostalAddress = postalAddress;
                executor.submit(() -> {
                    try {
                        Thread.sleep(2000);
                        sendBotMoFPResponse(1, String.valueOf(mofpWorkflowsId), "Message has been Received and Saved");
                        Thread.sleep(2000);
                        String tcbReference = "TCB" + DateUtil.now("yyyyMMddHHmm");
                        sendAcctMaintenanceStatus((long) mofpMsgHeadSummaryId, tcbReference, finalName, finalPostalAddress);
                        Thread.sleep(2000);
                        sendBotMoFPResponse(2, String.valueOf(mofpWorkflowsId), "Message has been processed");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                executor.shutdown();
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("Threads didn't finish in 1 minute!");
                }
            } catch(DataAccessException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "OK";
    }

    public String closeMoFPAccountRequest(String payload) {
        int mofpWorkflowsId;
        try {
            Document document = XMLParserService.jaxbXMLToObject(payload, Document.class);
            LOGGER.info("Document {}", document);
            Header header = document.getHeader();
            LOGGER.info("Header {}", header);
            MsgSummary msgSummary = document.getMsgSummary();
            LOGGER.info("MsgSummary {}", msgSummary);
            try {
                String mainSql = "SELECT * FROM mofp_workflows WHERE msg_id = ?";
                List<Map<String, Object>> results = jdbcTemplate.queryForList(mainSql, header.getMsgId());
                String respStatus = "ACCEPTED";
                String description = "Message has been Received and Saved";
                if (!results.isEmpty()) {
                    respStatus = "REJECTED";
                    description = "Duplicate request";
                }

                KeyHolder keyHolder = new GeneratedKeyHolder();
                String sql = "INSERT INTO mofp_workflows (created_by, created_date, rec_status, sender, receiver, msg_id," +
                        " payment_type, message_type, bic, authority_ref, bot_ref, create_dt, expires_dt, branch_code," +
                        " status, nb_of_rec, resp_status, description) VALUES ('SYSTEM', ?, 'A', ?, ?, ?, ?, ?, ?, ?, ?," +
                        " ?, ?, ?, 'Pending Close', ?, ?, ?)";
                String finalSql = sql;
                String finalRespStatus = respStatus;
                String finalDescription = description;
                this.jdbcTemplate.update(connection -> {
                    PreparedStatement ps = connection.prepareStatement(finalSql, new String[] {"id"});
                    ps.setString(1, DateUtil.now());
                    ps.setString(2, header.getSender());
                    ps.setString(3, header.getReceiver());
                    ps.setString(4, header.getMsgId());
                    ps.setString(5, header.getPaymentType());
                    ps.setString(6, header.getMessageType());
                    ps.setString(7, msgSummary.getBic());
                    ps.setString(8, msgSummary.getAuthorityRef());
                    ps.setString(9, msgSummary.getBotRef());
                    ps.setDate(10, new java.sql.Date(msgSummary.getCreDtTm().getTime()));
                    ps.setDate(11, new java.sql.Date(msgSummary.getExprDt().getTime()));
                    ps.setString(12, "060");
                    ps.setInt(13, msgSummary.getNbOfRec());
                    ps.setString(14, finalRespStatus);
                    ps.setString(15, finalDescription);
                    return ps;
                }, keyHolder);
                mofpWorkflowsId = keyHolder.getKey().intValue();
                sql = "INSERT INTO mofp_msg_head_summary (sender, receiver, msgId, paymentType, messageType, bic," +
                        " authorityRef, botRef, creDtTm, exprDt, nbOfRec) VALUES (?, ?, ?, ?, ? ,?, ?, ?, ?, ?, ?)";
                String finalSql2 = sql;
                int mofpMsgHeadSummaryId;
                this.jdbcTemplate.update(connection -> {
                    PreparedStatement ps = connection.prepareStatement(finalSql2, new String[] {"id"});
                    ps.setString(1, header.getSender());
                    ps.setString(2, header.getReceiver());
                    ps.setString(3, header.getMsgId());
                    ps.setString(4, header.getPaymentType());
                    ps.setString(5, header.getMessageType());
                    ps.setString(6, msgSummary.getBic());
                    ps.setString(7, msgSummary.getAuthorityRef());
                    ps.setString(8, msgSummary.getBotRef());
                    ps.setDate(9,  new java.sql.Date(msgSummary.getCreDtTm().getTime()));
                    ps.setDate(10, new java.sql.Date(msgSummary.getExprDt().getTime()));
                    ps.setInt(11, msgSummary.getNbOfRec());
                    return ps;
                }, keyHolder);
                mofpMsgHeadSummaryId = keyHolder.getKey().intValue();
                Details details = document.getDetails();
                LOGGER.info("Details {}", details);
                List<AcctRecord> acctRecordList = details.getAcctRecord();
                String name = "";
                String postalAddress = "";
                for (AcctRecord acctRecord: acctRecordList) {
                    name = acctRecord.getName();
                    postalAddress = acctRecord.getPostalAddr();
                    String branchCode = this.jdbcTemplate.queryForObject("select branchCode from mofp_wf_accounts " +
                                    "where account_no = ?", new Object[] {acctRecord.getAcctNum()}, String.class);
                    if (Objects.equals(branchCode, "-1")) {
                        branchCode = "048170";
                    }
                    sql = "INSERT INTO mofp_messages (endToEndId, acctNum, name, ccy, transBic, transAcctNum, transAcctName," +
                            " transCcy, regionCode, district, phoneNum, email, postalAddr, closedAcctNum, branchCode," +
                            " msg_head_summary_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    this.jdbcTemplate.update(sql, new Object[]{acctRecord.getEndToEndId(), acctRecord.getAcctNum(),
                            acctRecord.getName(), acctRecord.getCcy(), acctRecord.getTransBic(), acctRecord.getTransAcctNum(),
                            acctRecord.getTransAcctName(), acctRecord.getTransCcy(), acctRecord.getRegionCode(),
                            acctRecord.getDistrict(), acctRecord.getPhoneNum(), acctRecord.getEmail(), acctRecord.getPostalAddr(),
                            acctRecord.getAcctNum(), branchCode, mofpMsgHeadSummaryId
                    });
                    // TESTING
                    try {
                        String sql1 = "UPDATE mofp_messages set closedAcctNum = ?, transferredAmt = ?, closeDtTm = ?, " +
                                "closingStatus = ?, closingStatusDesc = ? where endToEndId = ?";
                        jdbcTemplate.update(sql1, new Object[]{acctRecord.getAcctNum(), 1000000,
                                DateUtil.now(), "Closed", "Closed successfully", acctRecord.getEndToEndId()});
                    } catch (DataAccessException e) {
                        e.printStackTrace();
                    }
                    try {
                        sql = "UPDATE mofp_wf_accounts SET status = 'Pending Closing' WHERE account_no = ?";
                        this.jdbcTemplate.update(sql, acctRecord.getAcctNum());
                    } catch (DataAccessException e) {
                        e.printStackTrace();
                    }
                }

                Attachments attachments = document.getAttachments();
                if (attachments.getAttachment1() != null && !attachments.getAttachment1().isEmpty()) {
                    try {
                        sql = "INSERT INTO mofp_wf_attachments (created_by, created_date, rec_status, attachment," +
                                " mofp_workflows_id) VALUES ('SYSTEM', ?, 'A', ?, ?)";
                        jdbcTemplate.update(sql, new Object[]{DateUtil.now(), attachments.getAttachment1(), mofpWorkflowsId});
                    } catch (DataAccessException e) {
                        e.printStackTrace();
                    }
                }
                if (attachments.getAttachment2() != null && !attachments.getAttachment2().isEmpty()) {
                    try {
                        sql = "INSERT INTO mofp_wf_attachments (created_by, created_date, rec_status, attachment," +
                                " mofp_workflows_id) VALUES ('SYSTEM', ?, 'A', ?, ?)";
                        jdbcTemplate.update(sql, new Object[]{DateUtil.now(), attachments.getAttachment2(), mofpWorkflowsId});
                    } catch (DataAccessException e) {
                        e.printStackTrace();
                    }
                }
                ExecutorService executor = Executors.newFixedThreadPool(1);
                String finalName = name;
                String finalPostalAddress = postalAddress;
                executor.submit(() -> {
                    try {
                        Thread.sleep(2000);
                        sendBotMoFPResponse(1, String.valueOf(mofpWorkflowsId), "Message has been Received and Saved");
                        Thread.sleep(2000);
                        String tcbReference = "TCB" + DateUtil.now("yyyyMMddHHmm");
                        sendMoFPAccountClosingStatus((long) mofpMsgHeadSummaryId, tcbReference, finalName, finalPostalAddress);
                        Thread.sleep(2000);
                        sendBotMoFPResponse(2, String.valueOf(mofpWorkflowsId), "Message has been processed");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                executor.shutdown();
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("Threads didn't finish in 1 minute!");
                }
            } catch(DataAccessException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "OK";
    }

    public String cancelMoFPAccountRequest(String payload) {
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

            Document document = XMLParserService.jaxbXMLToObject(payload, Document.class);
            LOGGER.info("Document {}", document);
            Header header = document.getHeader();
            LOGGER.info("Header {}", header);
            CancelDetails cancelDetails = document.getCancelDetails();
            LOGGER.info("CancelDetails {}", cancelDetails);

            String sender = header.getSender();
            String receiver = header.getReceiver();
            String msgId = header.getMsgId();
            String paymentType = header.getPaymentType();
            String messageType = header.getMessageType();
            String createDt = cancelDetails.getCreDtTm();
            String orgMsgId = cancelDetails.getOrgMsgId();
            String orgPaymentType = cancelDetails.getPaymentType();
            String reason = cancelDetails.getReason();

            long longCreateDt = 0;
            try {
                java.util.Date d = f.parse(createDt);
                longCreateDt = d.getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            String reference = "CXL" + DateUtil.now("yyyyMMddHHmmss");
            try {
                KeyHolder keyHolder = new GeneratedKeyHolder();
                String sql = "INSERT INTO mofp_workflows (created_by, created_date, rec_status, sender, receiver, msg_id," +
                        " payment_type, message_type, bic, authority_ref, bot_ref, create_dt, expires_dt, branch_code," +
                        " status, nb_of_rec, resp_status, cancel_reason) VALUES ('SYSTEM', ?, 'A', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?," +
                        " 'Pending Cancel', ?, 'ACCEPTED', ?)";
                String finalSql = sql;
                long finalLongCreateDt = longCreateDt;
                this.jdbcTemplate.update(connection -> {
                    PreparedStatement ps = connection.prepareStatement(finalSql, new String[] {"id"});
                    ps.setString(1, DateUtil.now());
                    ps.setString(2, sender);
                    ps.setString(3, receiver);
                    ps.setString(4, msgId);
                    ps.setString(5, paymentType);
                    ps.setString(6, messageType);
                    ps.setString(7, "TAPBTZTZ");
                    ps.setString(8, reference);
                    ps.setString(9, reference);
                    ps.setDate(10, new java.sql.Date(finalLongCreateDt));
                    ps.setDate(11, new java.sql.Date(Calendar.getInstance().getTime().getTime()));
                    ps.setString(12, "060");
                    ps.setInt(13, 0);
                    ps.setString(14, reason);
                    return ps;
                }, keyHolder);
                int mofpWorkflowsId = keyHolder.getKey().intValue();
                sql = "INSERT INTO mofp_msg_head_summary (sender, receiver, msgId, paymentType, messageType, bic," +
                        " authorityRef, botRef, creDtTm, exprDt, nbOfRec) VALUES (?, ?, ?, ?, ? ,?, ?, ?, ?, ?, ?)";
                String finalSql2 = sql;
                this.jdbcTemplate.update(connection -> {
                    PreparedStatement ps = connection.prepareStatement(finalSql2, new String[] {"id"});
                    ps.setString(1, sender);
                    ps.setString(2, receiver);
                    ps.setString(3, msgId);
                    ps.setString(4, paymentType);
                    ps.setString(5, messageType);
                    ps.setString(6, "TAPBTZTZ");
                    ps.setString(7, reference);
                    ps.setString(8, reference);
                    ps.setDate(9, new java.sql.Date(finalLongCreateDt));
                    ps.setDate(10, new java.sql.Date(Calendar.getInstance().getTime().getTime()));
                    ps.setInt(11, 1);
                    return ps;
                }, keyHolder);
                sql = "UPDATE mofp_workflows SET status = 'Cancelled', description = ? where msg_id = ?";
                this.jdbcTemplate.update(sql, new Object[] {reason, orgMsgId});
                try {
                    sql = "SELECT id from mofp_workflows where msg_id = ? and payment_type = ?";
                    Integer orgMofpWorkflowsId = this.jdbcTemplate.queryForObject(sql, Integer.class,
                            new Object[]{orgMsgId, orgPaymentType});
                    sql = "UPDATE mofp_wf_accounts SET status = 'Cancelled' WHERE mofp_workflows_id = ?";
                    this.jdbcTemplate.update(sql, orgMofpWorkflowsId);
                } catch (EmptyResultDataAccessException e) {
                    sql = "UPDATE mofp_workflows SET resp_status = 'REJECTED', description = ? WHERE id = ?";
                    this.jdbcTemplate.update(sql, "This request does not exist", mofpWorkflowsId);
                }
                new Thread(() -> {
                    try {
                        sendBotMoFPResponse(1, String.valueOf(mofpWorkflowsId), "");
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).start();
            } catch(DataAccessException e) {
                e.printStackTrace();
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return "OK";
    }

    public String accountBalanceRequest(String payload) {
        int mofpWorkflowsId;
        try {
            Document document = XMLParserService.jaxbXMLToObject(payload, Document.class);
            LOGGER.info("Document {}", document);
            Header header = document.getHeader();
            LOGGER.info("Header {}", header);
            RequestSummary requestSummary = document.getRequestSummary();
            LOGGER.info("RequestSummary {}", requestSummary);
            String dt = requestSummary.getCreDtTm().split("T")[0];
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-M-d");
            LocalDate parsedDate = LocalDate.parse(dt, inputFormatter);
            java.sql.Date sqlDate = java.sql.Date.valueOf(parsedDate);
            try {
                String respStatus = "ACCEPTED";
                String description = "Message has been Received and Saved";
                KeyHolder keyHolder = new GeneratedKeyHolder();
                String sql = "INSERT INTO mof_msg_request (created_by, rec_status, sender, receiver, msg_id," +
                        " payment_type, message_type, request_id, create_dt, account_num, resp_status, description)" +
                        " VALUES ('SYSTEM', 'A', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                this.jdbcTemplate.update(connection -> {
                    PreparedStatement ps = connection.prepareStatement(sql, new String[] {"id"});
                    ps.setString(1, header.getSender());
                    ps.setString(2, header.getReceiver());
                    ps.setString(3, header.getMsgId());
                    ps.setString(4, header.getPaymentType());
                    ps.setString(5, header.getMessageType());
                    ps.setString(6, requestSummary.getRequestId());
                    ps.setDate(7, sqlDate);
                    ps.setString(8, requestSummary.getAccountNum());
                    ps.setString(9, respStatus);
                    ps.setString(10, description);
                    return ps;
                }, keyHolder);
                mofpWorkflowsId = keyHolder.getKey().intValue();
                ExecutorService executor = Executors.newFixedThreadPool(1);
                executor.submit(() -> {
                    try {
                        Thread.sleep(2000);
                        sendAccountBalance(requestSummary.getRequestId(), requestSummary.getAccountNum());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
                executor.shutdown();
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("Threads didn't finish in 1 minute!");
                }
                return sendMoFResponse(String.valueOf(mofpWorkflowsId), "Message has been Received and Saved");
            } catch (DataAccessException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "OK";
    }

    public void sendAccountBalance(String msgId, String account) {
        LOGGER.info("Message ID: {}", msgId);
        try {
            if (Objects.equals(account.trim(), "ALL")) {
                String sql = "SELECT a.ACCT_NO as account, v.AVAILABLE_BALANCE as balance, a.REC_ST as status, c.CRNCY_CD as currency FROM " +
                        "V_DP_AVAILABLE_BAL v, ACCOUNT a, CURRENCY c WHERE a.CRNCY_ID = c.CRNCY_ID and " +
                        "a.ACCT_NO = v.ACCT_NO and a.ACCT_NO IN ('420400000058','430400000046','430400000217'," +
                        "'420400000088','430400000144','420400000304','420400000298','420400000313','420428000021'," +
                        "'420428000036','430428000071','420428000101','420428000091','430410000006','190207000015'," +
                        "'110239000034','200208000001','285239000001','110239000033','130239000001','173208000063'," +
                        "'173208000067','420437000007','420438000003','420432000269','420432000232','420432000631'," +
                        "'420432000117','420432000129','420432000130','420432000131','420432000132','420432000133'," +
                        "'420432000134','420432000135','420432000486','420432000159','420434000065','420434000066'," +
                        "'420434000069','170228000011','170242000001','282208000012','223208000008','280227000017'," +
                        "'180227000014','180208000016','150227000020','150208000013','270208000001','330208000006'," +
                        "'191208000002','170208000001','262207000096','220208000006','191208000032','420208000001'," +
                        "'390227000003','190227000010','170208000020','260208000002','170208000003','130208000004'," +
                        "'110208000113','110208000114','420400000296','223208000001','240208000002','130030000563'," +
                        "'130030000642','130030000631','130030000598','190030000180','190030000192','190030000187'," +
                        "'470030000068','470030000067','173030000582','470030000078','470030000076','110030001318'," +
                        "'110030001276','173030000637','173030000613','450030000135','200030000372','200030000361'," +
                        "'200030000380','200030000378','285030000362','285030000363','110030001239','110030001220'," +
                        "'110030001241','110030001184','110030001317','110030001313','110030001299','130040000064'," +
                        "'450040000016','223244000001','341244000001','330244000001','173244000002','251244000001'," +
                        "'240244000001','173244000003','350244000001','222208000001','130244000001','220244000001'," +
                        "'330244000002','130245000001','420410000002','420410000041','420410000042','190227000012'," +
                        "'420410000019','420410000020','420410000021','470410000003','430410000007','430410000008'," +
                        "'430410000009','430410000010','130410000001','420410000005','420410000006','420410000007'," +
                        "'420410000008','420410000013','430410000002','430410000003','430410000004','420410000032'," +
                        "'430410000015','430410000016','450410000021','420410000003','420410000046','420410000047'," +
                        "'470410000007','420425000002','420410000023','420410000024','420410000036','420400002266'," +
                        "'420410000045','450410000018','450410000019','190227000009','470410000005','420410000014'," +
                        "'420410000049','214400000001','420423000001','430423000004','420423000002','420423000003'," +
                        "'430423000001','430423000002','430423000003','420434000131','430423000006','420423000133'," +
                        "'450423000004','430431000001','420431000001','420431000002','420431000003','420429000008'," +
                        "'430416000043','480416000005','420403000001','430427000001','430425000002','420425000003'," +
                        "'420425000004','420425000007','430434000002','430428000057','340227000001','410208000016'," +
                        "'410208000019','410208000020','410208000022','410208000024','410208000026','410208000028'," +
                        "'410208000030','340207000014','340208000009','130030000635','430410000011','450423000001'," +
                        "'162204000399','160218000321','440411000002')";
                List<Map<String, Object>> list = jdbcLiveTemplate.queryForList(sql);
                Document document = new Document();
                Header header = new Header();
                header.setMsgId("TCB" + DateUtil.now("yyyyMMddHHmmss"));
                header.setSender("TAPBTZTZ");
                header.setReceiver("MOFPTZTZ");
                header.setMessageType("AccountBalance");
                header.setPaymentType("P309");
                document.setHeader(header);
                MsgSummary msgSummary = new MsgSummary();
                msgSummary.setNbOfRec(list.size());
                msgSummary.setCreDtTm(new Date());
                msgSummary.setOrgRequestId(msgId);
                document.setMsgSummary(msgSummary);
                document.setTrxRecords(new ArrayList<>());
                for (Map<String, Object> map: list) {
                    TrxRecord trxRecord = new TrxRecord();
                    trxRecord.setAcctNum((String) map.get("account"));
                    trxRecord.setCurrency((String) map.get("currency"));
                    trxRecord.setAccountStatus((String) map.get("status"));
                    int val = ((BigDecimal) map.get("balance")).compareTo(BigDecimal.ZERO);
                    String indicator;
                    if (val < 0) indicator = "DR"; else indicator = "CR";
                    trxRecord.setBalCdtDbtInd(indicator);
                    trxRecord.setBalance((BigDecimal) map.get("balance"));
                    document.getTrxRecords().add(trxRecord);
                }
                LOGGER.info("Document response {}", document);
                try {
                    sendMoFMessage("BALANCE", document);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                String sql = "SELECT v.AVAILABLE_BALANCE as balance, a.REC_ST as status, c.CRNCY_CD as currency FROM " +
                        "V_DP_AVAILABLE_BAL v, ACCOUNT a, CURRENCY c WHERE a.CRNCY_ID = c.CRNCY_ID and " +
                        "a.ACCT_NO = v.ACCT_NO and a.ACCT_NO = ?";
                Map<String, Object> map = new HashMap<>();
                jdbcLiveTemplate.queryForObject(sql, (rs, rowNum) -> {
                    map.put("balance", rs.getBigDecimal("balance"));
                    map.put("status", rs.getString("status"));
                    map.put("currency", rs.getString("currency"));
                    int val = rs.getBigDecimal("balance").compareTo(BigDecimal.ZERO);
                    String indicator;
                    if (val < 0) indicator = "DR"; else indicator = "CR";
                    map.put("indicator", indicator);
                    return map;
                }, account);
                LOGGER.info("Message ID: {}", msgId);
                Document document = new Document();
                Header header = new Header();
                header.setMsgId("TCB" + DateUtil.now("yyyyMMddHHmmss"));
                header.setSender("TAPBTZTZ");
                header.setReceiver("MOFPTZTZ");
                header.setMessageType("AccountBalance");
                header.setPaymentType("P309");
                document.setHeader(header);
                MsgSummary msgSummary = new MsgSummary();
                msgSummary.setNbOfRec(1);
                msgSummary.setCreDtTm(new Date());
                msgSummary.setOrgRequestId(msgId);
                document.setMsgSummary(msgSummary);
                document.setTrxRecords(new ArrayList<>());
                TrxRecord trxRecord = new TrxRecord();
                trxRecord.setAcctNum(account);
                trxRecord.setCurrency((String) map.get("currency"));
                trxRecord.setAccountStatus((String) map.get("status"));
                trxRecord.setBalCdtDbtInd((String) map.get("indicator"));
                trxRecord.setBalance((BigDecimal) map.get("balance"));
                document.getTrxRecords().add(trxRecord);
                LOGGER.info("Document response {}", document);
                try {
                    sendMoFMessage("BALANCE", document);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        } catch(DataAccessException e) {
            e.printStackTrace();
        }
    }

    public void sendTransactionSummary() {
        try {
            String sql = "SELECT payorAcct AS account, currency, paymentChannel, COUNT(id) AS totalTransactions," +
                    " SUM(trxAmount) AS totalAmount FROM batch_transactions where status = 'C' and payorAcct IN " +
                    "('20110002338','420400000058','430400000046','430400000217','420400000088','430400000144'," +
                    "'420400000304','420400000298','420400000313','420428000021'," +
                    "'420428000036','430428000071','420428000101','420428000091','430410000006','190207000015'," +
                    "'110239000034','200208000001','285239000001','110239000033','130239000001','173208000063'," +
                    "'173208000067','420437000007','420438000003','420432000269','420432000232','420432000631'," +
                    "'420432000117','420432000129','420432000130','420432000131','420432000132','420432000133'," +
                    "'420432000134','420432000135','420432000486','420432000159','420434000065','420434000066'," +
                    "'420434000069','170228000011','170242000001','282208000012','223208000008','280227000017'," +
                    "'180227000014','180208000016','150227000020','150208000013','270208000001','330208000006'," +
                    "'191208000002','170208000001','262207000096','220208000006','191208000032','420208000001'," +
                    "'390227000003','190227000010','170208000020','260208000002','170208000003','130208000004'," +
                    "'110208000113','110208000114','420400000296','223208000001','240208000002','130030000563'," +
                    "'130030000642','130030000631','130030000598','190030000180','190030000192','190030000187'," +
                    "'470030000068','470030000067','173030000582','470030000078','470030000076','110030001318'," +
                    "'110030001276','173030000637','173030000613','450030000135','200030000372','200030000361'," +
                    "'200030000380','200030000378','285030000362','285030000363','110030001239','110030001220'," +
                    "'110030001241','110030001184','110030001317','110030001313','110030001299','130040000064'," +
                    "'450040000016','223244000001','341244000001','330244000001','173244000002','251244000001'," +
                    "'240244000001','173244000003','350244000001','222208000001','130244000001','220244000001'," +
                    "'330244000002','130245000001','420410000002','420410000041','420410000042','190227000012'," +
                    "'420410000019','420410000020','420410000021','470410000003','430410000007','430410000008'," +
                    "'430410000009','430410000010','130410000001','420410000005','420410000006','420410000007'," +
                    "'420410000008','420410000013','430410000002','430410000003','430410000004','420410000032'," +
                    "'430410000015','430410000016','450410000021','420410000003','420410000046','420410000047'," +
                    "'470410000007','420425000002','420410000023','420410000024','420410000036','420400002266'," +
                    "'420410000045','450410000018','450410000019','190227000009','470410000005','420410000014'," +
                    "'420410000049','214400000001','420423000001','430423000004','420423000002','420423000003'," +
                    "'430423000001','430423000002','430423000003','420434000131','430423000006','420423000133'," +
                    "'450423000004','430431000001','420431000001','420431000002','420431000003','420429000008'," +
                    "'430416000043','480416000005','420403000001','430427000001','430425000002','420425000003'," +
                    "'420425000004','420425000007','430434000002','430428000057','340227000001','410208000016'," +
                    "'410208000019','410208000020','410208000022','410208000024','410208000026','410208000028'," +
                    "'410208000030','340207000014','340208000009','130030000635','430410000011','450423000001'," +
                    "'162204000399','160218000321','440411000002') GROUP BY account, paymentChannel";
            List<Map<String, Object>> list = jdbcBrinjalTemplate.queryForList(sql);
            Document document = new Document();
            Header header = new Header();
            header.setMsgId("TCB" + DateUtil.now("yyyyMMddHHmmss"));
            header.setSender("TAPBTZTZ");
            header.setReceiver("MOFPTZTZ");
            header.setMessageType("TransactionSummary");
            header.setPaymentType("P310");
            document.setHeader(header);
            MsgSummary msgSummary = new MsgSummary();
            msgSummary.setNbOfRec(list.size());
            msgSummary.setCreDtTm(new Date());
            document.setMsgSummary(msgSummary);
            document.setTrxRecords(new ArrayList<>());
            System.out.println("List: " + list);
            try {
                for (Map<String, Object> map : list) {
                    TrxRecord trxRecord = new TrxRecord();
                    trxRecord.setAcctNum((String) map.get("account"));
                    String currency = (String) map.get("currency");
                    if (currency.contains(".")) {
                        String[] arr = currency.split("\\.");
                        trxRecord.setCurrency(arr[1]);
                    } else {
                        trxRecord.setCurrency(currency);
                    }
                    Object value = map.get("totalTransactions");
                    if (value instanceof Integer) {
                        Integer totalTransactions = (Integer) value;
                        trxRecord.setNbOfRecTiss(totalTransactions);
                    } else {
                        trxRecord.setNbOfRecTiss(0);
                    }
                    trxRecord.setNbOfRecTiss(0);
                    trxRecord.setNbOfRecSwift(0);
                    trxRecord.setNbOfRecClearance(0);
                    trxRecord.setNbOfRecCash(0);
                    trxRecord.setNbOfRecEft(0);
                    trxRecord.setNbOfRecTips(0);
                    trxRecord.setNbOfRecWallet(0);
                    trxRecord.setNbOfRecInternalTransfer(0);
                    document.getTrxRecords().add(trxRecord);
                }
                LOGGER.info("Document response {}", document);
                try {
                    sendMoFMessage("SUMMARY", document);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } catch(DataAccessException e) {
            e.printStackTrace();
        }
    }

    public String bankStatementRequest(String payload) {
        int mofpWorkflowsId;
        try {
            Document document = XMLParserService.jaxbXMLToObject(payload, Document.class);
            LOGGER.info("Document {}", document);
            Header header = document.getHeader();
            LOGGER.info("Header {}", header);
            RequestSummary requestSummary = document.getRequestSummary();
            LOGGER.info("RequestSummary {}", requestSummary);
            String dt = requestSummary.getCreDtTm().split("T")[0];
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-M-d");
            LocalDate parsedDate = LocalDate.parse(dt, inputFormatter);
            java.sql.Date sqlDate = java.sql.Date.valueOf(parsedDate);
            try {
                String respStatus = "ACCEPTED";
                String description = "Message has been Received and Saved";
                KeyHolder keyHolder = new GeneratedKeyHolder();
                String sql = "INSERT INTO mof_msg_request (created_by, rec_status, sender, receiver, msg_id," +
                        " payment_type, message_type, request_id, create_dt, account_num, resp_status, description)" +
                        " VALUES ('SYSTEM', 'A', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                this.jdbcTemplate.update(connection -> {
                    PreparedStatement ps = connection.prepareStatement(sql, new String[] {"id"});
                    ps.setString(1, header.getSender());
                    ps.setString(2, header.getReceiver());
                    ps.setString(3, header.getMsgId());
                    ps.setString(4, header.getPaymentType());
                    ps.setString(5, header.getMessageType());
                    ps.setString(6, requestSummary.getRequestId());
                    ps.setDate(7, sqlDate);
                    ps.setString(8, requestSummary.getAccountNum());
                    ps.setString(9, respStatus);
                    ps.setString(10, description);
                    return ps;
                }, keyHolder);
                mofpWorkflowsId = keyHolder.getKey().intValue();
                ExecutorService executor = Executors.newFixedThreadPool(1);
                executor.submit(() -> {
                    try {
                        Thread.sleep(2000);
                        sendBankStatement(header.getMsgId(), requestSummary.getAccountNum());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
                executor.shutdown();
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("Threads didn't finish in 1 minute!");
                }
                return sendMoFResponse(String.valueOf(mofpWorkflowsId), "Message has been Received and Saved");
            } catch (DataAccessException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "OK";
    }

    public void sendBankStatement(String msgId, String account) {
        LOGGER.info("Message ID: {}", msgId);
        try {
            String sql = "SELECT a.ACCT_NO as account, a.ACCT_NM as accountName, v.AVAILABLE_BALANCE as balance, a.REC_ST as status, c.CRNCY_CD as currency FROM " +
                    "V_DP_AVAILABLE_BAL v, ACCOUNT a, CURRENCY c WHERE a.CRNCY_ID = c.CRNCY_ID and " +
                    "a.ACCT_NO = v.ACCT_NO and a.ACCT_NO IN ('420400000058','430400000046','430400000217'," +
                    "'420400000088','430400000144','420400000304','420400000298')";
            List<Map<String, Object>> list = jdbcLiveTemplate.queryForList(sql);
            LOGGER.info("Message ID: {}", msgId);
            Document document = new Document();
            Header header = new Header();
            header.setMsgId("TCB" + DateUtil.now("yyyyMMddHHmmss"));
            header.setSender("TAPBTZTZ");
            header.setReceiver("MOFPTZTZ");
            header.setMessageType("STATEMENT");
            header.setPaymentType("P105");
            document.setHeader(header);
            StatementSummary msgSummary = new StatementSummary();
            msgSummary.setAcctName("ACCOUNT NAME");
            msgSummary.setAcctNum(account);
            msgSummary.setCurrency("TZS");
            msgSummary.setCloseBal(BigDecimal.ZERO);
            msgSummary.setOpenBal(BigDecimal.ZERO);
            msgSummary.setOpenCdtDbtInd("CR");
            msgSummary.setCloseCdtDbtInd("CR");
            msgSummary.setCreDtTm(new Date());
            msgSummary.setSmtDt(DateUtil.now("yyyy-MM-dd"));
            document.setStatementSummary(msgSummary);
            document.setStatementRecords(new ArrayList<>());
            for (Map<String, Object> map: list) {
                StatementRecord statementRecord = new StatementRecord();
                statementRecord.setBankRef(DateUtil.now("yyyyMMddHHmmss"));
                statementRecord.setRelatedRef("TCB" + DateUtil.now("yyyyMMddHHmmss"));
                statementRecord.setTranType("CR");
                statementRecord.setExchangeRate("1");
                statementRecord.setTrxAmount(new BigDecimal("11015871"));
                statementRecord.setTranCode("GEPL");
                statementRecord.setDescription("GEPG");
                document.getStatementRecords().add(statementRecord);
            }
            LOGGER.info("Document response {}", document);
            try {
                sendMoFMessage("STATEMENT", document);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } catch(DataAccessException e) {
            e.printStackTrace();
        }
    }

    private void sendMoFMessage(String messageType, Document document) throws Exception {
        String request = XMLParserService.jaxbGenericObjToXML(document, Boolean.FALSE, Boolean.FALSE);
        switch (messageType) {
            case "BALANCE":
                if (request != null) {
                    request = request.replace("<Document>", "<Document xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                            " xsi:noNamespaceSchemaLocation=\"schema_account_balance.xsd\">");
                }
                LOGGER.info("MoF ACCOUNT(S) BALANCE RESPONSE:{}", request);
                break;
            case "SUMMARY":
                if (request != null) {
                    request = request.replace("<Document>", "<Document xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                            " xsi:noNamespaceSchemaLocation=\"schema_transaction_summary.xsd\">");
                }
                LOGGER.info("MoF TRANSACTION SUMMARY RESPONSE:{}", request);
                break;
            case "STATEMENT":
                if (request != null) {
                    request = request.replace("<Document>", "<Document xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                            " xsi:noNamespaceSchemaLocation=\"schema_bank_statement.xsd\">");
                }
                LOGGER.info("MoF BANK STATEMENT RESPONSE:{}", request);
                break;
        }
        String msgToBeSigned = request.trim().replaceAll("\\n+", "").replaceAll("\\t+", "");
        String signature = SignRequest.generateSignature256(msgToBeSigned, "bank.12345",
                "tcbbank", sysenv.MOF_CASH_MANAGEMENT_PFX_PRIVATE_KEY_FILE_PATH);
        String signedMsg = msgToBeSigned.trim() + "|" + signature;
        LOGGER.info("Signed Response {}", signedMsg);

        String domain;
        if (sysenv.ACTIVE_PROFILE.equalsIgnoreCase("prod")) {
            domain = "172.21.1.13";
        } else {
            domain = "172.21.2.12";
        }
        String url = "http://" + domain + ":8547/mof/api/mof/commercial-banks-response";
        String responseBody = "-1";
        LOGGER.info("Msg ID: " + document.getHeader().getMsgId());
        try {
            URL obj = new URL(url);
            LOGGER.info("Opening connection:" + url);
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/xml");
            connection.setRequestProperty("Cache-Control", "no-cache");

            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8);
            writer.write(signedMsg);
            writer.close();
            int responseCode = connection.getResponseCode();
            BufferedReader in;
            LOGGER.info("Response Code: " + responseCode);
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            LOGGER.info(response.toString());
            responseBody = response.toString();
            LOGGER.info("Closing connection: {}", url);
            connection.disconnect();
        } catch (IOException ex) {
            LOGGER.info(null, ex);
        }
        switch (messageType) {
            case "BALANCE":
                LOGGER.info("MoF BALANCE RESPONSE: {}", responseBody);
                break;
            case "SUMMARY":
                LOGGER.info("MoF SUMMARY RESPONSE:{}", responseBody);
                break;
            case "STATEMENT":
                LOGGER.info("MoF STATEMENT RESPONSE:{}", responseBody);
                break;
        }
    }

    public String getMoFPAccountsAjax(String branchCode, String branch, String fromDate, String toDate, String draw,
                                      String start, String rowPerPage, String searchValue, String columnIndex,
                                      String columnName, String columnSortOrder) {
        List<Map<String, Object>> results;
        String mainSql;
        int totalRecordWithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        if (fromDate == null || fromDate.length() < 10)
            fromDate = "2023-01-01 00:00:00";
        if (toDate == null || toDate.length() < 10)
            toDate = DateUtil.now("yyyy-MM-dd HH:mm:ss");
        try {
            boolean b = branchCode.equals("060") || Objects.equals(branchCode, "500") || Objects.equals(branchCode, "170") || Objects.equals(branchCode, "420");
            if (b) {
                if (branch == null || branch.isEmpty()) {
                    mainSql = "SELECT count(ma.id) FROM mofp_wf_accounts ma where ma.created_date>=? and ma.created_date<=? ";
                    totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{fromDate, toDate}, Integer.class);
                } else {
                    mainSql = "SELECT count(ma.id) FROM mofp_wf_accounts ma where ma.created_date>=? and ma.created_date<=? and substring(ma.branchCode, 4)=? ";
                    totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{fromDate, toDate, branch}, Integer.class);
                }
            } else {
                mainSql = "SELECT count(ma.id) FROM mofp_wf_accounts ma where ma.created_date>=? and ma.created_date<=? and substring(ma.branchCode, 4)=? ";
                totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{fromDate, toDate, branchCode}, Integer.class);
            }
            String searchQuery;
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                if (b) {
                    searchQuery = " WHERE concat(ma.created_by,' ',ma.created_date,' ',ma.last_modified_by,' ',ma.last_modified_date,' ',ma.rec_status,' ',ma.account_no,' ',ma.account_title,' ',ma.email,' ',ma.phoneNum,' ',ma.regionCode,' ',ma.district,' ',ma.ccy) LIKE ? ";
                    totalRecordWithFilter = jdbcLiveTemplate.queryForObject("SELECT count(ma.id) FROM mofp_wf_accounts ma" + searchQuery, new Object[]{searchValue, fromDate, toDate}, Integer.class);
                    if (branch == null || branch.isEmpty()) {
                        mainSql = "SELECT ma.*, b.name branch FROM mofp_wf_accounts ma INNER JOIN branches b on substring(ma.branchCode, 4) = b.code " + searchQuery + " and ma.created_date>=? and ma.created_date<=? ORDER BY ma." + columnName + " " + columnSortOrder  + " limit " + start + "," + rowPerPage;
                        results = jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, fromDate, toDate});
                    } else {
                        mainSql = "SELECT ma.*, b.name branch FROM mofp_wf_accounts ma INNER JOIN branches b on substring(ma.branchCode, 4) = b.code " + searchQuery + "and ma.created_date>=? and ma.created_date<=? and substring(ma.branchCode, 4)=? ORDER BY ma." + columnName + " " + columnSortOrder  + " limit " + start + "," + rowPerPage;
                        results = jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, fromDate, toDate, branch});
                    }
                } else {
                    searchQuery = " WHERE concat(ma.created_by,' ',ma.created_date,' ',ma.last_modified_by,' ',ma.last_modified_date,' ',ma.rec_status,' ',ma.account_no,' ',ma.account_title,' ',ma.email,' ',ma.phoneNum,' ',ma.regionCode,' ',ma.district,' ',ma.ccy) LIKE ? and substring(ma.branchCode, 4)=? and ma.created_date>=? and ma.created_date<=? ";
                    totalRecordWithFilter = jdbcKycTemplate.queryForObject("SELECT count(ma.id) FROM mofp_wf_accounts ma" + searchQuery, new Object[]{searchValue, branchCode, fromDate, toDate}, Integer.class);
                    mainSql = "SELECT ma.*, b.name branch FROM mofp_wf_accounts ma INNER JOIN branches b on substring(ma.branchCode, 4) = b.code" + searchQuery + "  ORDER BY ma." + columnName + " " + columnSortOrder  + " limit " + start + "," + rowPerPage;
                    results = jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, branchCode, fromDate, toDate});
                }
            } else {
                totalRecordWithFilter = totalRecords;
                if (b) {
                    if (branch == null || branch.isEmpty()) {
                        mainSql = "SELECT ma.*, b.name branch FROM mofp_wf_accounts ma INNER JOIN branches b on substring(ma.branchCode, 4) = b.code where ma.created_date>=? and ma.created_date<=? ORDER BY ma." + columnName + " " + columnSortOrder  + " limit " + start + "," + rowPerPage;
                        results = jdbcTemplate.queryForList(mainSql, new Object[]{fromDate, toDate});
                    } else {
                        mainSql = "SELECT ma.*, b.name branch FROM mofp_wf_accounts ma INNER JOIN branches b on substring(ma.branchCode, 4) = b.code where ma.created_date>=? and ma.created_date<=? and substring(ma.branchCode, 4)=? ORDER BY ma." + columnName + " " + columnSortOrder  + " limit " + start + "," + rowPerPage;
                        results = jdbcTemplate.queryForList(mainSql, new Object[]{fromDate, toDate, branch});
                    }
                } else {
                    mainSql = "SELECT ma.*, b.name branch FROM mofp_wf_accounts ma inner join branches b on substring(ma.branchCode, 4) = b.code where ma.created_date>=? and ma.created_date<=? and substring(ma.branchCode, 4)=? ORDER BY ma." + columnName + " " + columnSortOrder  + " limit " + start + "," + rowPerPage;
                    results = jdbcTemplate.queryForList(mainSql, new Object[]{fromDate, toDate, branchCode});
                }
            }
            jsonString = jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        return "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordWithFilter + "\",\"aaData\":" + jsonString + "}";
    }

    public String assignMoFPAccount(String id, String username, String accountNo, String nbOfTxn) {
        Connection connection = null;
        try {
            if (jdbcTemplate.getDataSource() != null) {
                connection = jdbcTemplate.getDataSource().getConnection();
                CallableStatement callableStatement = connection.prepareCall("{call wfStatusChange(?, ?, ?, ?, ?)}");
                callableStatement.setString(1, username);
                callableStatement.setString(2, DateUtil.now("yyyy-MM-dd HH:mm:ss"));
                callableStatement.setString(3, "Awaiting Checker");
                callableStatement.setInt(4, Integer.parseInt(id));
                callableStatement.setInt(5, Integer.parseInt(nbOfTxn));
                int rows = callableStatement.executeUpdate();
                if (rows > 0) {
                    String sql = "update mofp_wf_accounts set account_no = ?, last_modified_by = ?, last_modified_date = ? where id = ?";
                    jdbcTemplate.update(sql, accountNo, username, DateUtil.now(), id);
                    return "{\"success\":true}";
                } else {
                    return "{\"success\":false}";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return "{\"success\":false}";
    }

    public String rejectMoFPAccount(String id, String username, String nbOfTxn, String comments) {
        Connection connection = null;
        try {
            if (jdbcTemplate.getDataSource() != null) {
                connection = jdbcTemplate.getDataSource().getConnection();
                CallableStatement callableStatement = connection.prepareCall("{call wfStatusChange(?, ?, ?, ?, ?)}");
                callableStatement.setString(1, username);
                callableStatement.setString(2, DateUtil.now("yyyy-MM-dd HH:mm:ss"));
                callableStatement.setString(3, "Rejected");
                callableStatement.setInt(4, Integer.parseInt(id));
                callableStatement.setInt(5, Integer.parseInt(nbOfTxn));
                int rows = callableStatement.executeUpdate();
                if (rows > 0) {
                    sendBotMoFPResponse(1, id, comments);
                    return "{\"success\":true}";
                } else {
                    return "{\"success\":false}";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return "{\"success\":false}";
    }

    public String updateMoFPAccount(String id, String username, String nbOfTxn, String comments) {
        Connection connection = null;
        try {
            if (jdbcTemplate.getDataSource() != null) {
                connection = jdbcTemplate.getDataSource().getConnection();
                CallableStatement callableStatement = connection.prepareCall("{call wfStatusChange(?, ?, ?, ?, ?)}");
                callableStatement.setString(1, username);
                callableStatement.setString(2, DateUtil.now("yyyy-MM-dd HH:mm:ss"));
                callableStatement.setString(3, "Updated");
                callableStatement.setInt(4, Integer.parseInt(id));
                callableStatement.setInt(5, Integer.parseInt(nbOfTxn));
                int rows = callableStatement.executeUpdate();
                if (rows > 0) {
                    Document document = new Document();
                    jdbcTemplate.queryForObject("select * from mofp_workflows where id = ?", (rs, rowNum) -> {
                        Header header = new Header();
                        header.setMsgId(rs.getString("msg_id") + "_1");
                        header.setSender(rs.getString("sender"));
                        header.setReceiver(rs.getString("receiver"));
                        header.setMessageType(rs.getString("message_type"));
                        header.setPaymentType("P305");
                        document.setHeader(header);
                        MsgSummary msgSummary = new MsgSummary();
                        msgSummary.setAuthorityRef(rs.getString("authority_ref"));
                        msgSummary.setBic(rs.getString("bic"));
                        msgSummary.setBotRef(rs.getString("bot_ref"));
                        msgSummary.setNbOfRec(rs.getInt("nb_of_rec"));
                        msgSummary.setCreDtTm(rs.getDate("create_dt"));
                        document.setMsgSummary(msgSummary);

                        return document;
                    }, id);
                    Details details = new Details();
                    List<AcctRecord> acctRecords = new ArrayList<>();
                    jdbcTemplate.query("select * from mofp_wf_accounts where mofp_workflows_id = ?", (rs, rowNum) -> {
                        AcctRecord acctRecord = new AcctRecord();
                        acctRecord.setAcctNum(rs.getString("account_no"));
                        acctRecord.setOldName(rs.getString("account_title"));
                        acctRecord.setNewName(rs.getString("transAcctName"));
                        acctRecord.setCcy(rs.getString("ccy"));
                        acctRecord.setChangeDtTm(DateUtil.now());
                        acctRecord.setChangingStatus("Changed");
                        acctRecord.setChangingStatusDesc("Changed successfully");
                        acctRecord.setRegionCode(rs.getString("regionCode"));
                        acctRecord.setDistrict(rs.getString("district"));
                        acctRecord.setPhoneNum(rs.getString("phoneNum"));
                        acctRecord.setEmail(rs.getString("email"));
                        acctRecord.setPostalAddr(rs.getString("postalAddr"));
                        acctRecords.add(acctRecord);
                        return document;
                    }, id);
                    details.setAcctRecord(acctRecords);
                    document.setDetails(details);
                    String acctNo = document.getDetails().getAcctRecord().get(0).getAcctNum();
                    String openDate = document.getDetails().getAcctRecord().get(0).getOpenDt();
                    Attachments attachments = new Attachments();
                    String tcbReference = "TCB" + DateUtil.now("yyyyMMddHHmmss");
                    String institutionName = document.getDetails().getAcctRecord().get(0).getName();
                    String addressee = document.getDetails().getAcctRecord().get(0).getPostalAddr();
                    attachments.setAttachment1(generateChangeAccountLetter(tcbReference, institutionName, addressee,
                            getFormatLetterDate(openDate), Long.parseLong(id)));
                    document.setAttachments(attachments);
                    LOGGER.info("Document: {}", document.getAttachments().getAttachment1());
                    LOGGER.info("Document response {}", document);
                    String token = botMoFPLogin();
//                    String token = "ETCQw3CVPYu5MCT9tAeg87v4Wd8ROiQgHPYQ3Js/IG2pYQcQdptvtwfCG4X5HLN2MIEG5eU6rK+vrf40B246I5rEBCbDe+784q+E211Rg6u09NsMb1BFw/HWpLSzYL4OUCS9lK12IE0kb2epSxKf7xP7OVVZ7f2t45aXhyvlGl6B24VUS08gsd3IuCVOHEYsHXPpnlkZW0a0Y4X+5ln/5SI1AzzhdQ2wzT5npqb0bbSRy6572hlwFq8mko2u+a+JDC0piAuFTWTF20WbZJIkUwf28ElQpZVaHJqkbS9ZZYNA2bXISFxg7tW8hQxPJC7zdSlYrD1nNTfuU95l/TcS8A==";
                    sendBotMoFPMessage("ACCOUNTMAINTENANCESTATUS", token, document);
                    return "{\"success\":true}";
                } else {
                    return "{\"success\":false}";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return "{\"success\":false}";
    }

    public String acceptMoFPAccount(String id, String nbOfTxn, String username) {
        Connection connection = null;
        try {
            if (jdbcTemplate.getDataSource() != null) {
                connection = jdbcTemplate.getDataSource().getConnection();
                CallableStatement callableStatement = connection.prepareCall("{call wfStatusChange(?, ?, ?, ?, ?)}");
                callableStatement.setString(1, username);
                callableStatement.setString(2, DateUtil.now("yyyy-MM-dd HH:mm:ss"));
                callableStatement.setString(3, "Received");
                callableStatement.setInt(4, Integer.parseInt(id));
                callableStatement.setInt(5, Integer.parseInt(nbOfTxn));
                int rows = callableStatement.executeUpdate();
                if (rows > 0) {
                    try {
                        String sql = "UPDATE mofp_wf_accounts SET status_desc = ? where id = ?";
                        int updateRes = jdbcKycTemplate.update(sql, "Account opened successfully!", id);
                        if (updateRes > 0) {
                            return "{\"success\":true}";
                        } else {
                            return "{\"success\":false}";
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        return "{\"success\":false}";
                    }
                } else {
                    return "{\"success\":false}";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return "{\"success\":false}";
    }

    public String acceptMoFPAccountRequest(String id, String username) {
        try {
            String sql = "update mofp_workflows set last_modified_by = ?, last_modified_date = ?, status = ? where id = ?";
            int rows = jdbcTemplate.update(sql, username, DateUtil.now(), "Received", id);
            if (rows == 1) {
                return "{\"success\":true}";
            } else {
                return "{\"success\":false}";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "{\"success\":false}";
    }

    public String sendMoFPAccountRequestToBranch(String id, String username) {
        try {
            String sql = "update mofp_workflows mw inner join mofp_wf_accounts ma on ma.mofp_workflows_id = mw.id set" +
                    " ma.last_modified_by = ?, ma.last_modified_date = ?, ma.status = ?," +
                    " mw.last_modified_by = ?, mw.last_modified_date = ?, mw.status = ? where mw.id = ?";
            int rows = jdbcTemplate.update(sql, username, DateUtil.now(), "Awaiting Maker", username, DateUtil.now(),
                    "Awaiting Maker", id);
            if (rows > 0) {
                return "{\"success\":true}";
            } else {
                return "{\"success\":false}";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "{\"success\":false}";
    }

    public String confirmMoFPAccount(String id, String username, String nbOfTxn) {
        Connection connection = null;
        try {
            if (jdbcTemplate.getDataSource() != null) {
                connection = jdbcTemplate.getDataSource().getConnection();
                CallableStatement callableStatement = connection.prepareCall("{call wfStatusChange(?, ?, ?, ?, ?)}");
                callableStatement.setString(1, username);
                callableStatement.setString(2, DateUtil.now("yyyy-MM-dd HH:mm:ss"));
                callableStatement.setString(3, "Accepted");
                callableStatement.setInt(4, Integer.parseInt(id));
                callableStatement.setInt(5, Integer.parseInt(nbOfTxn));
                int rows = callableStatement.executeUpdate();
                if (rows > 1) {
                    String status = jdbcTemplate.queryForObject("select status from mofp_workflows where id = ?", String.class, id);
                    if (Objects.equals(status, "Accepted")) {
                        Document document = new Document();
                        jdbcTemplate.queryForObject("select * from mofp_workflows where id = ?", (rs, rowNum) -> {
                            Header header = new Header();
                            header.setMsgId(rs.getString("msg_id") + "_1");
                            header.setSender(rs.getString("sender"));
                            header.setReceiver(rs.getString("receiver"));
                            header.setMessageType(rs.getString("message_type"));
                            header.setPaymentType("P301");
                            document.setHeader(header);
                            MsgSummary msgSummary = new MsgSummary();
                            msgSummary.setAuthorityRef(rs.getString("authority_ref"));
                            msgSummary.setBic(rs.getString("bic"));
                            msgSummary.setBotRef(rs.getString("bot_ref"));
                            msgSummary.setNbOfRec(rs.getInt("nb_of_rec"));
                            msgSummary.setCreDtTm(rs.getDate("create_dt"));
                            document.setMsgSummary(msgSummary);

                            return document;
                        }, id);
                        Details details = new Details();
                        List<AcctRecord> acctRecords = new ArrayList<>();
                        jdbcTemplate.query("select * from mofp_wf_accounts where mofp_workflows_id = ?", (rs, rowNum) -> {
                            AcctRecord acctRecord = new AcctRecord();
                            acctRecord.setOrgEndToEndId(rs.getString("endtoEndId"));
                            acctRecord.setName(rs.getString("account_title"));
                            acctRecord.setCcy(rs.getString("ccy"));
                            acctRecord.setAcctNum(rs.getString("account_no"));
                            acctRecord.setOpenDt(DateUtil.now());
                            acctRecord.setOpeningStatus("Accepted");
                            acctRecord.setOpeningStatusDesc(rs.getString("status_desc"));
                            acctRecords.add(acctRecord);
                            return document;
                        }, id);
                        details.setAcctRecord(acctRecords);
                        document.setDetails(details);
                        String openDate = document.getDetails().getAcctRecord().get(0).getOpenDt();
                        Attachments attachments = new Attachments();
                        String tcbReference = "TCB" + DateUtil.now("yyyyMMddHHmmss");
                        String institutionName = document.getDetails().getAcctRecord().get(0).getName();
                        String addressee = document.getDetails().getAcctRecord().get(0).getPostalAddr();
                        attachments.setAttachment1(generateNewAccountLetter(tcbReference, institutionName, addressee,
                                getFormatLetterDate(openDate), Long.parseLong(id)));
                        document.setAttachments(attachments);
                        LOGGER.info("Document: {}", document.getAttachments().getAttachment3());
                        LOGGER.info("Document response {}", document);
                        String token = botMoFPLogin();
//                        String token = "ETCQw3CVPYu5MCT9tAeg87v4Wd8ROiQgHPYQ3Js/IG2pYQcQdptvtwfCG4X5HLN2MIEG5eU6rK+vrf40B246I5rEBCbDe+784q+E211Rg6u09NsMb1BFw/HWpLSzYL4OUCS9lK12IE0kb2epSxKf7xP7OVVZ7f2t45aXhyvlGl6B24VUS08gsd3IuCVOHEYsHXPpnlkZW0a0Y4X+5ln/5SI1AzzhdQ2wzT5npqb0bbSRy6572hlwFq8mko2u+a+JDC0piAuFTWTF20WbZJIkUwf28ElQpZVaHJqkbS9ZZYNA2bXISFxg7tW8hQxPJC7zdSlYrD1nNTfuU95l/TcS8A==";
                        sendBotMoFPMessage("ACCOUNTOPENINGSTATUS", token, document);
                    }
                    return "{\"success\":true}";
                } else {
                    return "{\"success\":false}";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return "{\"success\":false}";
    }

    private String botMoFPLogin() {
        String token;
        String request;
        String domain;
        String apiKey;
        if (sysenv.ACTIVE_PROFILE.equalsIgnoreCase("prod")) {
            domain = "172.21.1.13";
            request = "{" +
                    "\"username\": \"TAPBTZTZ\"," +
                    "\"password\": \"Prod@123#\"" +
                    "}";
            apiKey = "651UbDuJB0GQjH9vhDaLRlHhdu8oS+jefGWQJ74mbg85npFXTlqCxhMXzr8WJpTVqbgGNT5LGn1sHJ4M6kY87w==:FPWsPwo4dknfPyMtL/zKL7OojKjOOwp0dbLin0qyKbReFEWiSPfohzocOV7hwyACnnJr1UXvH5LxB+Q8Vjt7WYSxS4yIhq72lp7ZxsYOINVzlAI90Tx51fvLA0J216HPnbO/YIi7iECIps7XHeYq62lRYusjAz8kV4uIjqZhRhAsOlsIlsH8BrCgN58jx4rA2bg7KD5ZHZH4+nJz+R1niQPtCR6QS574cfD2FCnkA4AVIfKsMqutKXHJib93eumebyaJBBuN4/bGlCE1oQjyYSBN0yUgxCmO8CLRLKsjNkyffyWIXl/WThVv/K+ojvRJtVgoz3SJSYDx5wKSPabB/Q==";
        } else {
            domain = "172.21.2.12";
            request = "{" +
                    "\"username\": \"TAPBTZT0\"," +
                    "\"password\": \"Test@123#\"" +
                    "}";
            apiKey = "BwWU3zNXcsgqWUmcvPtTgUY/wdgYW4efwzxv/p8+ujB9BgjU5c82R1nChVeCRPIh1N8+1A6D2eXuJ5yBrgDY+g==:xTSZmx5wX9w5wb7xK6tcmLYHN0yufgLWGYEirTvXG5ceWdoCpieLLv51KSSOne+yjvc+a/SqAtHlqswyvkBgTQrCnsALFJakvi5ng05z0SGnlrt+AAH7B2DoAC8u2K2KhU2Bg+HKWDTumHvu7tiI++N0ulEhUst7eGT7E01Zw+L1vcrYGH1cFozY5o/o4+Yfxw7ntOLyOyLq8LSF1lsAH3Hp86iLsZhifkwyvSeB8fJpA3mK0xcsgNkcJ4McSBPuylEfEl44nRM5wgaJpWjqVGsQJT+rMcg+IhTd8FCQ5qcunm0a6TpcRD1hUpp7o96KE0HQc+ml6vQ9wX7Oqel1wg==";
        }
        LOGGER.info("MoFP LOGIN REQUEST:{}", request);
        String url = "http://" + domain + ":8547/outgoing/api/v2/auth/login";
        String responseBody = "-1";
        String urlParameters = request.trim();
        try {
            URL obj = new URL(url);
            LOGGER.info("Opening connection:" + url);
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Cache-Control", "no-cache");
            connection.setRequestProperty("User-Agent", "PostmanRuntime/7.21.0");
            connection.setRequestProperty("x-api-key", apiKey);
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8);
            writer.write(urlParameters);
            writer.close();
            int responseCode = connection.getResponseCode();
            BufferedReader in = null;
            if (responseCode == 200) {
                in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            } else {
                LOGGER.info("Error Code: " + responseCode);
                in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            }
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            if (responseCode == 200) {
                responseBody = response.toString();
            } else {
                LOGGER.warn(response.toString());
            }
            LOGGER.info("Closing connection: {}", url);
            connection.disconnect();
        } catch (IOException ex) {
            LOGGER.info(null, ex);
        }
        LOGGER.info("MoFP LOGIN RESPONSE: {}", responseBody);
        JSONObject jsonObject = new JSONObject(responseBody);
        token = jsonObject.getString("token");
        return token;
    }

    private String sendBotMoFPMessage(String messageType, String token, Document document) throws Exception {
        String request = XMLParserService.jaxbGenericObjToXML(document, Boolean.FALSE, Boolean.FALSE);
        switch (messageType) {
            case "ACCOUNTOPENINGSTATUS":
                if (request != null) {
                    request = request.replace("<Document>", "<Document xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                            " xsi:noNamespaceSchemaLocation=\"account_opening_status.xsd\">");
                }
                LOGGER.info("MoFP ACCOUNT OPENING STATUS REQUEST:{}", request);
                break;
            case "ACCOUNTMAINTINANCESTATUS":
                if (request != null) {
                    request = request.replace("<Document>", "<Document xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                            " xsi:noNamespaceSchemaLocation=\"account_maintenance_status.xsd\">");
                }
                LOGGER.info("MoFP ACCOUNT MAINTENANCE STATUS REQUEST:{}", request);
                break;
            case "ACCOUNTCLOSINGSTATUS":
                if (request != null) {
                    request = request.replace("<Document>", "<Document xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                            " xsi:noNamespaceSchemaLocation=\"account_closing_status.xsd\">");
                }
                LOGGER.info("MoFP ACCOUNT CLOSING STATUS REQUEST:{}", request);
                break;
        }
        String msgToBeSigned = request.trim().replaceAll("\\n+", "").replaceAll("\\t+", "");
        String signature = SignRequest.generateSignature256(msgToBeSigned, "bank.12345",
                "tcbbank", sysenv.MOF_CASH_MANAGEMENT_PFX_PRIVATE_KEY_FILE_PATH);
        String signedMsg = msgToBeSigned + "|" + signature;
        LOGGER.info("Response {}", signedMsg);
        String domain;
        String apiKey;
        if (sysenv.ACTIVE_PROFILE.equalsIgnoreCase("prod")) {
            domain = "172.21.1.13";
            apiKey = "651UbDuJB0GQjH9vhDaLRlHhdu8oS+jefGWQJ74mbg85npFXTlqCxhMXzr8WJpTVqbgGNT5LGn1sHJ4M6kY87w==:FPWsPwo4dknfPyMtL/zKL7OojKjOOwp0dbLin0qyKbReFEWiSPfohzocOV7hwyACnnJr1UXvH5LxB+Q8Vjt7WYSxS4yIhq72lp7ZxsYOINVzlAI90Tx51fvLA0J216HPnbO/YIi7iECIps7XHeYq62lRYusjAz8kV4uIjqZhRhAsOlsIlsH8BrCgN58jx4rA2bg7KD5ZHZH4+nJz+R1niQPtCR6QS574cfD2FCnkA4AVIfKsMqutKXHJib93eumebyaJBBuN4/bGlCE1oQjyYSBN0yUgxCmO8CLRLKsjNkyffyWIXl/WThVv/K+ojvRJtVgoz3SJSYDx5wKSPabB/Q==";
        } else {
            domain = "172.21.2.12";
            apiKey = "BwWU3zNXcsgqWUmcvPtTgUY/wdgYW4efwzxv/p8+ujB9BgjU5c82R1nChVeCRPIh1N8+1A6D2eXuJ5yBrgDY+g==:xTSZmx5wX9w5wb7xK6tcmLYHN0yufgLWGYEirTvXG5ceWdoCpieLLv51KSSOne+yjvc+a/SqAtHlqswyvkBgTQrCnsALFJakvi5ng05z0SGnlrt+AAH7B2DoAC8u2K2KhU2Bg+HKWDTumHvu7tiI++N0ulEhUst7eGT7E01Zw+L1vcrYGH1cFozY5o/o4+Yfxw7ntOLyOyLq8LSF1lsAH3Hp86iLsZhifkwyvSeB8fJpA3mK0xcsgNkcJ4McSBPuylEfEl44nRM5wgaJpWjqVGsQJT+rMcg+IhTd8FCQ5qcunm0a6TpcRD1hUpp7o96KE0HQc+ml6vQ9wX7Oqel1wg==";
        }
        String url = "http://" + domain + ":8547/outgoing/api/v2/message";
        String responseBody = "-1";
        LOGGER.info("Msg ID: " + document.getHeader().getMsgId());
        try {
            URL obj = new URL(url);
            LOGGER.info("Opening connection:" + url);
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/xml");
            connection.setRequestProperty("Cache-Control", "no-cache");
            connection.setRequestProperty("Authorization", "Bearer " + token);
            connection.setRequestProperty("senderId", "TAPBTZTZ");
            connection.setRequestProperty("consumerId", "TANZTZTX");
            connection.setRequestProperty("messageId", document.getHeader().getMsgId());
            connection.setRequestProperty("messageType", messageType);
            connection.setRequestProperty("x-api-key", apiKey);
            connection.setRequestProperty("payloadType", "XML");

            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8);
            writer.write(signedMsg);
            writer.close();
            int responseCode = connection.getResponseCode();
            BufferedReader in;
            LOGGER.info("Response Code: " + responseCode);
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            LOGGER.info(response.toString());
            responseBody = response.toString();
            LOGGER.info("Closing connection: {}", url);
            connection.disconnect();
        } catch (IOException ex) {
            LOGGER.info(null, ex);
        }
        switch (messageType) {
            case "ACCOUNTOPENINGSTATUS":
                LOGGER.info("MoFP ACCOUNT OPENING STATUS RESPONSE: {}", responseBody);
                break;
            case "ACCOUNTMAINTINANCESTATUS":
                LOGGER.info("MoFP ACCOUNT MAINTENANCE STATUS RESPONSE:{}", responseBody);
                break;
            case "ACCOUNTCLOSINGSTATUS":
                LOGGER.info("MoFP ACCOUNT CLOSING STATUS RESPONSE:{}", responseBody);
                break;
        }
        return responseBody;
    }

    public String sendBotMoFPResponse(int i, String id, String comments) throws Exception {
        String messageId = null;
        String orgMessageId = null;
        String respStatus = null;
        String description = null;
        String paymentType = null;
        try {
            String mainSql = "SELECT * FROM mofp_workflows WHERE id = ?";
            Map<String, Object> result = jdbcTemplate.queryForMap(mainSql, id);
            LOGGER.info("Result {}", result);
            messageId = "TCB" + DateUtil.now("yyyyMMddHHmmss");
            orgMessageId = (String) result.get("msg_id");
            if (i == 1) {
                respStatus = (String) result.get("resp_status");
            } else if (i == 2) {
                respStatus = "PROCESSED";
            }
            description = (String) result.get("description");
            paymentType = (String) result.get("payment_type");
        } catch (Exception e) {
            e.printStackTrace();
        }

        String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<Document xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                " xsi:noNamespaceSchemaLocation=\"schema_response.xsd\">\n" +
                "\t<Header>\n" +
                "\t\t<Sender>TAPBTZTZ</Sender>\n" +
                "\t\t<Receiver>TANZTZTX</Receiver>\n" +
                "\t\t<MsgId>" + messageId + "</MsgId>\n" +
                "\t\t<PaymentType>" + paymentType + "</PaymentType>\n" +
                "\t\t<MessageType>RESPONSE</MessageType>\n" +
                "\t</Header>\n" +
                "\t<ResponseSummary>\n" +
                "\t\t<OrgMsgId>" + orgMessageId + "</OrgMsgId>\n" +
                "\t\t<CreDtTm>" + DateUtil.nowDateXMLGregorianCalendar() + "</CreDtTm>\n" +
                "\t\t<ResponseDetails>\n" +
                "\t\t\t<PaymentRef>NODATA</PaymentRef>\n" +
                "\t\t\t<RespStatus>" + respStatus.toUpperCase() + "</RespStatus>\n" +
                "\t\t\t<Description>" + (comments.isEmpty() ? description : comments) + "</Description>\n" +
                "\t\t</ResponseDetails>\n" +
                "\t</ResponseSummary>\n" +
                "</Document>\n";
        LOGGER.info("Request: {}", request);
        String msgToBeSigned = request.trim().replaceAll("\\n+", "").replaceAll("\\t+", "");
        String signature = SignRequest.generateSignature256(msgToBeSigned, "bank.12345",
                "tcbbank", sysenv.MOF_CASH_MANAGEMENT_PFX_PRIVATE_KEY_FILE_PATH);
        String signedMsg = msgToBeSigned + "|" + signature;
        LOGGER.info("Response {}", signedMsg);
        String token = botMoFPLogin();
        String domain;
        String apiKey;
        if (sysenv.ACTIVE_PROFILE.equalsIgnoreCase("prod")) {
            domain = "172.21.1.13";
            apiKey = "651UbDuJB0GQjH9vhDaLRlHhdu8oS+jefGWQJ74mbg85npFXTlqCxhMXzr8WJpTVqbgGNT5LGn1sHJ4M6kY87w==:FPWsPwo4dknfPyMtL/zKL7OojKjOOwp0dbLin0qyKbReFEWiSPfohzocOV7hwyACnnJr1UXvH5LxB+Q8Vjt7WYSxS4yIhq72lp7ZxsYOINVzlAI90Tx51fvLA0J216HPnbO/YIi7iECIps7XHeYq62lRYusjAz8kV4uIjqZhRhAsOlsIlsH8BrCgN58jx4rA2bg7KD5ZHZH4+nJz+R1niQPtCR6QS574cfD2FCnkA4AVIfKsMqutKXHJib93eumebyaJBBuN4/bGlCE1oQjyYSBN0yUgxCmO8CLRLKsjNkyffyWIXl/WThVv/K+ojvRJtVgoz3SJSYDx5wKSPabB/Q==";
        } else {
            domain = "172.21.2.12";
            apiKey = "BwWU3zNXcsgqWUmcvPtTgUY/wdgYW4efwzxv/p8+ujB9BgjU5c82R1nChVeCRPIh1N8+1A6D2eXuJ5yBrgDY+g==:xTSZmx5wX9w5wb7xK6tcmLYHN0yufgLWGYEirTvXG5ceWdoCpieLLv51KSSOne+yjvc+a/SqAtHlqswyvkBgTQrCnsALFJakvi5ng05z0SGnlrt+AAH7B2DoAC8u2K2KhU2Bg+HKWDTumHvu7tiI++N0ulEhUst7eGT7E01Zw+L1vcrYGH1cFozY5o/o4+Yfxw7ntOLyOyLq8LSF1lsAH3Hp86iLsZhifkwyvSeB8fJpA3mK0xcsgNkcJ4McSBPuylEfEl44nRM5wgaJpWjqVGsQJT+rMcg+IhTd8FCQ5qcunm0a6TpcRD1hUpp7o96KE0HQc+ml6vQ9wX7Oqel1wg==";
        }
        String url = "http://" + domain + ":8547/outgoing/api/v2/message";
        String responseBody = "-1";
        try {
            URL obj = new URL(url);
            LOGGER.info("Opening connection:" + url);
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/xml");
            connection.setRequestProperty("Cache-Control", "no-cache");
            connection.setRequestProperty("Authorization", "Bearer " + token);
            connection.setRequestProperty("senderId", "TAPBTZTZ");
            connection.setRequestProperty("consumerId", "TANZTZTX");
            connection.setRequestProperty("messageId", messageId);
            connection.setRequestProperty("messageType", "RESPONSE");
            connection.setRequestProperty("x-api-key", apiKey);
            connection.setRequestProperty("payloadType", "XML");

            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8);
            writer.write(signedMsg);
            writer.close();
            int responseCode = connection.getResponseCode();
            BufferedReader in;
            LOGGER.info("Response Code: " + responseCode);
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            LOGGER.info(response.toString());
            responseBody = response.toString();
            LOGGER.info("Closing connection: {}", url);
            connection.disconnect();
        } catch (IOException ex) {
            LOGGER.info(null, ex);
        }
        return responseBody;
    }

    public String sendMoFResponse(String id, String comments) throws Exception {
        String messageId = null;
        String orgMessageId = null;
        String respStatus = null;
        String description = null;
        String paymentType = null;
        try {
            String mainSql = "SELECT * FROM mof_msg_request WHERE id = ?";
            Map<String, Object> result = jdbcTemplate.queryForMap(mainSql, id);
            LOGGER.info("Result {}", result);
            messageId = "TCB" + DateUtil.now("yyyyMMddHHmmss");
            orgMessageId = (String) result.get("msg_id");
            respStatus = (String) result.get("resp_status");
            description = (String) result.get("description");
            paymentType = (String) result.get("payment_type");
        } catch (Exception e) {
            e.printStackTrace();
        }
        String msg = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<Document xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                " xsi:noNamespaceSchemaLocation=\"schema_response.xsd\">\n" +
                "\t<Header>\n" +
                "\t\t<Sender>TAPBTZTZ</Sender>\n" +
                "\t\t<Receiver>MOFPTZTZ</Receiver>\n" +
                "\t\t<MsgId>" + messageId + "</MsgId>\n" +
                "\t\t<PaymentType>" + paymentType + "</PaymentType>\n" +
                "\t\t<MessageType>RESPONSE</MessageType>\n" +
                "\t</Header>\n" +
                "\t<ResponseSummary>\n" +
                "\t\t<OrgMsgId>" + orgMessageId + "</OrgMsgId>\n" +
                "\t\t<CreDtTm>" + DateUtil.nowDateXMLGregorianCalendar() + "</CreDtTm>\n" +
                "\t\t<ResponseDetails>\n" +
                "\t\t\t<PaymentRef>NODATA</PaymentRef>\n" +
                "\t\t\t<RespStatus>" + respStatus.toUpperCase() + "</RespStatus>\n" +
                "\t\t\t<Description>" + (comments.isEmpty() ? description : comments) + "</Description>\n" +
                "\t\t</ResponseDetails>\n" +
                "\t</ResponseSummary>\n" +
                "</Document>\n";
        String msgToBeSigned = msg.trim().replaceAll("\\n+", "").replaceAll("\\t+", "");
        String signature = SignRequest.generateSignature256(msgToBeSigned, "bank.12345",
                "tcbbank", sysenv.MOF_CASH_MANAGEMENT_PFX_PRIVATE_KEY_FILE_PATH);
        String response = msgToBeSigned + "|" + signature;
        LOGGER.info("Response {}", response);
        return response;

    }

    public String generateNewAccountLetter(String tcbReference, String institutionName, String addressee,
                                            String openDate, Long id) {
        String reportFileTemplate = "/iReports/kyc/mofp_new_account.jasper";
        Connection conn = null;
        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("TCB_REFERENCE", tcbReference);
            parameters.put("INSTITUTION_NAME", institutionName);
            parameters.put("ADDRESSEE", addressee);
            parameters.put("OPEN_DATE", openDate);
            parameters.put("WF_ID", id);
            conn = cilantroDatasource.getConnection();
            JasperPrint print = jasperService.jasperPrint(reportFileTemplate, parameters, conn);
            conn.close();
//            try {
//                String sql = "UPDATE mofp_wf_accounts SET attachment = ? WHERE account_no = ?";
//                this.jdbcTemplate.update(sql, jasperService.exportPdfToStream(print).toByteArray(), accountNo);
//            } catch (DataAccessException e) {
//                e.printStackTrace();
//            }
            return Base64.encodeBase64String(jasperService.exportPdfToStream(print).toByteArray());
        } catch (IOException ex) {
            Logger.getLogger(KycRepo.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            Logger.getLogger(KycRepo.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public String generateChangeAccountLetter(String tcbReference, String institutionName, String addressee,
                                             String changeDate, Long id) {
        String reportFileTemplate = "/iReports/kyc/mofp_maintain_account.jasper";
        Connection conn = null;
        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("TCB_REFERENCE", tcbReference);
            parameters.put("INSTITUTION_NAME", institutionName);
            parameters.put("ADDRESSEE", addressee);
            parameters.put("CHANGE_DATE", changeDate);
            parameters.put("WF_ID", id);
            conn = cilantroDatasource.getConnection();
            JasperPrint print = jasperService.jasperPrint(reportFileTemplate, parameters, conn);
            conn.close();
//            try {
//                String sql = "UPDATE mofp_wf_accounts SET attachment = ? WHERE account_no = ?";
//                this.jdbcTemplate.update(sql, jasperService.exportPdfToStream(print).toByteArray(), accountNo);
//            } catch (DataAccessException e) {
//                e.printStackTrace();
//            }
            return Base64.encodeBase64String(jasperService.exportPdfToStream(print).toByteArray());
        } catch (IOException ex) {
            Logger.getLogger(KycRepo.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            Logger.getLogger(KycRepo.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public String generateCloseAccountLetter(String tcbReference, String institutionName, String addressee,
                                            String closeDate, Long id) {
        String reportFileTemplate = "/iReports/kyc/mofp_close_account.jasper";
        Connection conn = null;
        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("TCB_REFERENCE", tcbReference);
            parameters.put("INSTITUTION_NAME", institutionName);
            parameters.put("ADDRESSEE", addressee);
            parameters.put("CLOSE_DATE", closeDate);
            parameters.put("WF_ID", id);
            conn = cilantroDatasource.getConnection();
            JasperPrint print = jasperService.jasperPrint(reportFileTemplate, parameters, conn);
            conn.close();
//            try {
//                String sql = "UPDATE mofp_wf_accounts SET attachment = ? WHERE account_no = ?";
//                this.jdbcTemplate.update(sql, jasperService.exportPdfToStream(print).toByteArray(), accountNo);
//            } catch (DataAccessException e) {
//                e.printStackTrace();
//            }
            return Base64.encodeBase64String(jasperService.exportPdfToStream(print).toByteArray());
        } catch (IOException ex) {
            Logger.getLogger(KycRepo.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            Logger.getLogger(KycRepo.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public String generateLetter(Long id, String tcbReference, String institutionName, String addressee, String type) {
        Map<String, Object> map = jdbcTemplate.queryForMap("select * from mofp_messages where msg_head_summary_id = ?",  id);
        String base64Str = null;
        if (Objects.equals(type, "New")) {
            String openDate = ((java.time.LocalDateTime) map.get("openDt")).format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            base64Str = generateNewAccountLetter(tcbReference, institutionName, addressee,
                    getFormatLetterDate(openDate), id);
        } else if (Objects.equals(type, "Close")) {
            String closeDate = ((java.time.LocalDateTime) map.get("closeDtTm")).format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            base64Str = generateCloseAccountLetter(tcbReference, institutionName, addressee,
                    getFormatLetterDate(closeDate), id);
        } else if (Objects.equals(type, "Change")) {
            String changeDate = ((java.time.LocalDateTime) map.get("changeDtTm")).format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            base64Str = generateChangeAccountLetter(tcbReference, institutionName, addressee,
                    getFormatLetterDate(changeDate), id);
        }
        LOGGER.info("Document: {}", base64Str);
        return base64Str;
    }

    private String getFormatLetterDate(String openDate) {
        String[] suffixes = {
                "0th",  "1st",  "2nd",  "3rd",  "4th",  "5th",  "6th",  "7th",  "8th",  "9th",
                "10th", "11th", "12th", "13th", "14th", "15th", "16th", "17th", "18th", "19th",
                "20th", "21st", "22nd", "23rd", "24th", "25th", "26th", "27th", "28th", "29th",
                "30th", "31st"
        };
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date openDt;
        try {
            openDt = simpleDateFormat.parse(openDate);
            Calendar c = Calendar.getInstance();
            c.setTime(openDt);
            int day = c.get(Calendar.DAY_OF_MONTH);
            return suffixes[day] + " " + new SimpleDateFormat("MMMM, yyyy").format(openDt);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public String sendMoFPAccountOpeningStatus(Long id, String tcbReference, String institutionName, String addressee) throws Exception {
        LOGGER.info("ID: {}", id);
        Document document = new Document();
        jdbcTemplate.queryForObject("select * from mofp_msg_head_summary where id = ?", (rs, rowNum) -> {
            Header header = new Header();
            header.setMsgId(tcbReference);
            header.setSender(rs.getString("receiver"));
            header.setReceiver(rs.getString("sender"));
            header.setMessageType("AccountOpeningStatus");
            header.setPaymentType("P301");
            document.setHeader(header);
            MsgSummary msgSummary = new MsgSummary();
            msgSummary.setBic(rs.getString("bic"));
            msgSummary.setRef(rs.getString("botRef"));
            msgSummary.setNbOfRec(rs.getInt("nbOfRec"));
            msgSummary.setCreDtTm(rs.getDate("creDtTm"));
            document.setMsgSummary(msgSummary);

            return document;
        }, id);
        Details details = new Details();
        List<AcctRecord> acctRecords = new ArrayList<>();
        jdbcTemplate.query("select * from mofp_messages where msg_head_summary_id = ?", (rs, rowNum) -> {
            AcctRecord acctRecord = new AcctRecord();
            String endToEndId = rs.getString("orgEndToEndId") == null ? "" : rs.getString("orgEndToEndId");
            acctRecord.setOrgEndToEndId(endToEndId);
            acctRecord.setName(rs.getString("name"));
            acctRecord.setCcy(rs.getString("ccy"));
            acctRecord.setAcctNum(rs.getString("acctNum"));
            acctRecord.setOpenDt(rs.getString("openDt"));
            acctRecord.setOpeningStatus(rs.getString("openingStatus"));
            acctRecord.setOpeningStatusDesc(rs.getString("openingStatusDesc"));
            acctRecords.add(acctRecord);
            return document;
        }, id);
        details.setAcctRecord(acctRecords);
        document.setDetails(details);
        String openDate = document.getDetails().getAcctRecord().get(0).getOpenDt();
        Attachments attachments = new Attachments();
        attachments.setAttachment1(generateNewAccountLetter(tcbReference, institutionName, addressee,
                getFormatLetterDate(openDate), id));
        document.setAttachments(attachments);
        LOGGER.info("Document response {}", document);
        try {
            String sql = "INSERT INTO mofp_wf_attachments (created_by, created_date, rec_status, attachment," +
                    " mofp_workflows_id) VALUES ('SYSTEM', ?, 'A', ?, ?)";
            jdbcTemplate.update(sql, new Object[]{DateUtil.now(), attachments.getAttachment1(), id});
        } catch (DataAccessException e) {
            e.printStackTrace();
        }
        String token = botMoFPLogin();
        return sendBotMoFPMessage("ACCOUNTOPENINGSTATUS", token, document);
    }

    public String sendMoFPAccountClosingStatus(Long id, String tcbReference, String institutionName, String addressee) throws Exception {
        Document document = new Document();
        jdbcTemplate.queryForObject("select * from mofp_msg_head_summary where id = ?", (rs, rowNum) -> {
            Header header = new Header();
            header.setMsgId(tcbReference);
            header.setSender(rs.getString("receiver"));
            header.setReceiver(rs.getString("sender"));
            header.setMessageType("AccountClosingStatus");
            header.setPaymentType("P303");
            document.setHeader(header);
            MsgSummary msgSummary = new MsgSummary();
            msgSummary.setBic(rs.getString("bic"));
            msgSummary.setRef(rs.getString("botRef"));
            msgSummary.setNbOfRec(rs.getInt("nbOfRec"));
            msgSummary.setCreDtTm(rs.getDate("creDtTm"));
            document.setMsgSummary(msgSummary);

            return document;
        }, id);
        Details details = new Details();
        List<AcctRecord> acctRecords = new ArrayList<>();
        jdbcTemplate.query("select * from mofp_messages where msg_head_summary_id = ?", (rs, rowNum) -> {
            AcctRecord acctRecord = new AcctRecord();
            acctRecord.setOrgEndToEndId(rs.getString("orgEndToEndId"));
            acctRecord.setClosingStatus(rs.getString("closingStatus"));
            acctRecord.setClosingStatusDesc(rs.getString("closingStatusDesc"));
            acctRecord.setClosedAcctNum(rs.getString("closedAcctNum"));
            acctRecord.setName(rs.getString("name"));
            acctRecord.setCcy(rs.getString("ccy"));
            acctRecord.setCloseDtTm(rs.getString("closeDtTm"));
            acctRecord.setTransBic(rs.getString("transBic"));
            acctRecord.setTransAcctNum(rs.getString("transAcctNum"));
            acctRecord.setTransAcctName(rs.getString("transAcctName"));
            acctRecord.setTransCcy(rs.getString("transCcy"));
            acctRecord.setTransferredAmt(rs.getString("transferredAmt"));
            acctRecords.add(acctRecord);
            return document;
        }, id);
        details.setAcctRecord(acctRecords);
        document.setDetails(details);
        String closeDate = document.getDetails().getAcctRecord().get(0).getCloseDtTm();
        Attachments attachments = new Attachments();
        attachments.setAttachment1(generateCloseAccountLetter(tcbReference, institutionName, addressee,
                getFormatLetterDate(closeDate), id));
        document.setAttachments(attachments);
        LOGGER.info("Document response {}", document);
        try {
            String sql = "INSERT INTO mofp_wf_attachments (created_by, created_date, rec_status, attachment," +
                    " mofp_workflows_id) VALUES ('SYSTEM', ?, 'A', ?, ?)";
            jdbcTemplate.update(sql, new Object[]{DateUtil.now(), attachments.getAttachment1(), id});
        } catch (DataAccessException e) {
            e.printStackTrace();
        }
        String token = botMoFPLogin();
        return sendBotMoFPMessage("ACCOUNTCLOSINGSTATUS", token, document);
    }

    public String sendRejectedResponse(String payLoad) {
        String sender = XMLParserService.getDomTagText("Sender", payLoad);
        String receiver = XMLParserService.getDomTagText("Receiver", payLoad);
        String msgId = XMLParserService.getDomTagText("MsgId", payLoad);
        String ref = XMLParserService.getDomTagText("BotRef", payLoad);
        String respRef = "RESP" + DateUtil.now("yyyyMMddHHmmss");
        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<Document xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "          xsi:noNamespaceSchemaLocation=\"schema_response.xsd\">\n" +
                "\t<Header>\n" +
                "\t\t<Sender>"+receiver+"</Sender>\n" +
                "\t\t<Receiver>"+sender+"</Receiver>\n" +
                "\t\t<MsgId>"+respRef+"</MsgId>\n" +
                "\t\t<PaymentType>P306</PaymentType>\n" +
                "\t\t<MessageType>RESPONSE</MessageType>\n" +
                "\t</Header>\n" +
                "\t<ResponseSummary>\n" +
                "\t\t<OrgMsgId>"+msgId+"</OrgMsgId>\n" +
                "\t\t<CreDtTm>"+DateUtil.nowDateXMLGregorianCalendar()+"</CreDtTm>\n" +
                "\t\t<ResponseDetails>\n" +
                "\t\t\t<PaymentRef>"+ref+"</PaymentRef>\n" +
                "\t\t\t<RespStatus>REJECTED</RespStatus>\n" +
                "\t\t\t<Description>Failed</Description>\n" +
                "\t\t</ResponseDetails>\n" +
                "\t</ResponseSummary>\n" +
                "</Document>\n";
        LOGGER.info("Response: {}", response);
        return response;
    }

    public String sendAcctMaintenanceStatus(Long id, String tcbReference, String institutionName, String addressee) throws Exception {
        Document document = new Document();
        jdbcTemplate.queryForObject("select * from mofp_msg_head_summary where id = ?", (rs, rowNum) -> {
            Header header = new Header();
            header.setMsgId(tcbReference);
            header.setSender(rs.getString("receiver"));
            header.setReceiver(rs.getString("sender"));
            header.setMessageType("AccountMaintenanceStatus");
            header.setPaymentType("P305");
            document.setHeader(header);
            MsgSummary msgSummary = new MsgSummary();
            msgSummary.setBic(rs.getString("bic"));
            msgSummary.setRef(rs.getString("botRef"));
            msgSummary.setNbOfRec(rs.getInt("nbOfRec"));
            msgSummary.setCreDtTm(rs.getDate("creDtTm"));
            document.setMsgSummary(msgSummary);

            return document;
        }, id);
        Details details = new Details();
        List<AcctRecord> acctRecords = new ArrayList<>();
        jdbcTemplate.query("select * from mofp_messages where msg_head_summary_id = ?", (rs, rowNum) -> {
            AcctRecord acctRecord = new AcctRecord();
            acctRecord.setAcctNum(rs.getString("acctNum"));
            acctRecord.setOldName(rs.getString("oldName"));
            acctRecord.setNewName(rs.getString("newName"));
            acctRecord.setCcy(rs.getString("ccy"));
            acctRecord.setChangeDtTm(rs.getString("changeDtTm"));
            acctRecord.setChangingStatus(rs.getString("changingStatus"));
            acctRecord.setChangingStatusDesc(rs.getString("changingStatusDesc"));
            acctRecord.setRegionCode(rs.getString("regionCode"));
            acctRecord.setDistrict(rs.getString("district"));
            acctRecord.setPhoneNum(rs.getString("phoneNum"));
            acctRecord.setEmail(rs.getString("email"));
            acctRecord.setPostalAddr(rs.getString("postalAddr"));
            acctRecords.add(acctRecord);
            return document;
        }, id);
        details.setAcctRecord(acctRecords);
        document.setDetails(details);
        String changeDate = document.getDetails().getAcctRecord().get(0).getChangeDtTm();
        Attachments attachments = new Attachments();
        attachments.setAttachment1(generateChangeAccountLetter(tcbReference, institutionName, addressee,
                getFormatLetterDate(changeDate), id));
        document.setAttachments(attachments);
        LOGGER.info("Document response {}", document);
        try {
            String sql = "INSERT INTO mofp_wf_attachments (created_by, created_date, rec_status, attachment," +
                    " mofp_workflows_id) VALUES ('SYSTEM', ?, 'A', ?, ?)";
            jdbcTemplate.update(sql, new Object[]{DateUtil.now(), attachments.getAttachment1(), id});
        } catch (DataAccessException e) {
            e.printStackTrace();
        }
        String token = botMoFPLogin();
        return sendBotMoFPMessage("ACCOUNTMAINTENANCESTATUS", token, document);
    }

    public String closeMoFPAccount(String id, String username, String nbOfTxn) {
        Connection connection = null;
        try {
            if (jdbcTemplate.getDataSource() != null) {
                connection = jdbcTemplate.getDataSource().getConnection();
                CallableStatement callableStatement = connection.prepareCall("{call wfStatusChange(?, ?, ?, ?, ?)}");
                callableStatement.setString(1, username);
                callableStatement.setString(2, DateUtil.now("yyyy-MM-dd HH:mm:ss"));
                callableStatement.setString(3, "Closed");
                callableStatement.setInt(4, Integer.parseInt(id));
                callableStatement.setInt(5, Integer.parseInt(nbOfTxn));
                int rows = callableStatement.executeUpdate();
                if (rows > 0) {
                    String status = jdbcTemplate.queryForObject("select status from mofp_workflows where id = ?", String.class, id);
                    if (Objects.equals(status, "Closed")) {
                        Document document = new Document();
                        jdbcTemplate.queryForObject("select * from mofp_workflows where id = ?", (rs, rowNum) -> {
                            Header header = new Header();
                            header.setMsgId(rs.getString("msg_id") + "_1");
                            header.setSender(rs.getString("sender"));
                            header.setReceiver(rs.getString("receiver"));
                            header.setMessageType(rs.getString("message_type"));
                            header.setPaymentType("P303");
                            document.setHeader(header);
                            MsgSummary msgSummary = new MsgSummary();
                            msgSummary.setAuthorityRef(rs.getString("authority_ref"));
                            msgSummary.setBic(rs.getString("bic"));
                            msgSummary.setBotRef(rs.getString("bot_ref"));
                            msgSummary.setNbOfRec(rs.getInt("nb_of_rec"));
                            msgSummary.setCreDtTm(rs.getDate("create_dt"));
                            document.setMsgSummary(msgSummary);

                            return document;
                        }, id);
                        Details details = new Details();
                        List<AcctRecord> acctRecords = new ArrayList<>();
                        jdbcTemplate.query("select * from mofp_wf_accounts where mofp_workflows_id = ?", (rs, rowNum) -> {
                            AcctRecord acctRecord = new AcctRecord();
                            acctRecord.setOrgEndToEndId(rs.getString("endToEndId"));
                            acctRecord.setClosingStatus("Closed");
                            acctRecord.setClosingStatusDesc("Closed successfully");
                            acctRecord.setClosedAcctNum(rs.getString("account_no"));
                            acctRecord.setName(rs.getString("account_title"));
                            acctRecord.setCcy(rs.getString("ccy"));
                            acctRecord.setCloseDtTm(DateUtil.now());
                            acctRecord.setTransBic(rs.getString("transBic"));
                            acctRecord.setTransAcctNum(rs.getString("transAcctNum"));
                            acctRecord.setTransAcctName(rs.getString("transAcctName"));
                            acctRecord.setTransCcy(rs.getString("transCcy"));
                            acctRecord.setTransferredAmt("0");
                            acctRecords.add(acctRecord);
                            return document;
                        }, id);
                        details.setAcctRecord(acctRecords);
                        document.setDetails(details);
                        String closeDate = document.getDetails().getAcctRecord().get(0).getCloseDtTm();
                        Attachments attachments = new Attachments();
                        String tcbReference = "TCB" + DateUtil.now("yyyyMMddHHmmss");
                        String institutionName = document.getDetails().getAcctRecord().get(0).getName();
                        String addressee = document.getDetails().getAcctRecord().get(0).getPostalAddr();
                        attachments.setAttachment1(generateCloseAccountLetter(tcbReference, institutionName, addressee,
                                getFormatLetterDate(closeDate), Long.parseLong(id)));
                        document.setAttachments(attachments);

                        LOGGER.info("Document: {}", document.getAttachments().getAttachment1());
                        LOGGER.info("Document response {}", document);
                        try {
                            String sql = "INSERT INTO mofp_wf_attachments (created_by, created_date, rec_status, attachment," +
                                    " mofp_workflows_id) VALUES ('SYSTEM', ?, 'A', ?, ?)";
                            jdbcTemplate.update(sql, new Object[]{DateUtil.now(), attachments.getAttachment1(), id});
                        } catch (DataAccessException e) {
                            e.printStackTrace();
                        }
                        String token = botMoFPLogin();
//                        String token = "ETCQw3CVPYu5MCT9tAeg87v4Wd8ROiQgHPYQ3Js/IG2pYQcQdptvtwfCG4X5HLN2MIEG5eU6rK+vrf40B246I5rEBCbDe+784q+E211Rg6u09NsMb1BFw/HWpLSzYL4OUCS9lK12IE0kb2epSxKf7xP7OVVZ7f2t45aXhyvlGl6B24VUS08gsd3IuCVOHEYsHXPpnlkZW0a0Y4X+5ln/5SI1AzzhdQ2wzT5npqb0bbSRy6572hlwFq8mko2u+a+JDC0piAuFTWTF20WbZJIkUwf28ElQpZVaHJqkbS9ZZYNA2bXISFxg7tW8hQxPJC7zdSlYrD1nNTfuU95l/TcS8A==";
                        sendBotMoFPMessage("ACCOUNTCLOSINGSTATUS", token, document);
                    }
                    return "{\"success\":true}";
                } else {
                    return "{\"success\":false}";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return "{\"success\":false}";
    }

    public String searchAgentAccount(String username, String account, String url) {
        String request = "{\"acctNo\": \"" + account + "\"}";
        String response = HttpClientService.sendJsonRequest(request, url);
        ExistingCustomer customer = new Gson().fromJson(response, ExistingCustomer.class);
        if (customer.getCustomerNumber() != null && !customer.getCustomerNumber().isEmpty())
            return "{\"responseCode\": 0, \"response\": " + response + "}";
        else return "{\"responseCode\": 99, \"response\": " + response + "}";
    }

    public List<Map<String, Object>> accountProducts() {
        try {
            return this.jdbcKycTemplate.queryForList("SELECT product_code, product_name, currency from " +
                    "account_products where rec_status = 'A'");
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            return null;
        }
    }

    public List<Map<String, Object>> getAgents() {
        try {
            return this.jdbcKycTemplate.queryForList("SELECT username, concat(firstname,' ',middlename,' ',lastname) " +
                    "as name from users where is_agent = 1 and rec_status = 'A'");
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            return null;
        }
    }

    public String getChampionVisits(String branchCode, String branch, String fromDate, String toDate, String draw,
                                    String start, String rowPerPage, String searchValue, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results;
        String mainSql;
        int totalRecordWithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        if (fromDate == null || fromDate.length() < 10)
            fromDate = "2021-10-01" + fromDate;
        if (toDate == null || toDate.length() < 10)
            toDate = DateUtil.now("yyyy-MM-dd HH:mm:ss");
        if (Objects.equals(rowPerPage, "-1")) {
            rowPerPage = "10";
        }
        try {
            if (branchCode.equals("060")) {
                if (branch == null || branch.isEmpty()) {
                    mainSql = "SELECT count(cv.id) FROM champion_visits_light cv inner join users u on cv.user_id = u.id inner join branches b on u.branch_code = b.code where cv.created_date>=? and cv.created_date<=? ";
                    totalRecords = jdbcKycTemplate.queryForObject(mainSql, new Object[]{fromDate, toDate}, Integer.class);
                } else {
                    mainSql = "SELECT count(cv.id) FROM champion_visits_light cv inner join users u on cv.user_id = u.id inner join branches b on u.branch_code = b.code where cv.created_date>=? and cv.created_date<=? and u.branch_code=? ";
                    totalRecords = jdbcKycTemplate.queryForObject(mainSql, new Object[]{fromDate, toDate, branch}, Integer.class);
                }
            } else {
                mainSql = "SELECT count(cv.id) FROM champion_visits_light cv inner join users u on cv.user_id = u.id inner join branches b on u.branch_code = b.code where cv.created_date>=? and cv.created_date<=? and u.branch_code=? ";
                totalRecords = jdbcKycTemplate.queryForObject(mainSql, new Object[]{fromDate, toDate, branchCode}, Integer.class);
            }
            String searchQuery;
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                if (branchCode.equals("060")) {
                    searchQuery = " WHERE concat(cv.champion,' ',cv.agent_name,' ',cv.agent_location,' ',cv.agent_phone,' ',b.code,' ',b.name,' ',cv.account,' ',cv.category) LIKE ? ";
                    totalRecordWithFilter = jdbcKycTemplate.queryForObject("SELECT count(cv.id) FROM champion_visits_light cv inner join users u on cv.user_id = u.id inner join branches b on u.branch_code = b.code" + searchQuery + "and cv.created_date>=? and cv.created_date<=? ", new Object[]{searchValue, fromDate, toDate}, Integer.class);
                    if (branch == null || branch.isEmpty()) {
                        mainSql = "select cv.id, concat(u.firstname, ' ', u.middlename, ' ', u.lastname) as champion, cv.name as agent_name, cv.msisdn as agent_phone, cv.location as agent_location, cv.captured_location, cv.captured_latitude, cv.captured_longitude, cv.activity_performed, cv.remarks_collected, cv.created_by, cv.created_date as visit_date, cv.account, cv.category, cv.signature, cv.location_photo, b.code, b.name from champion_visits_light cv inner join users u on cv.user_id = u.id inner join branches b on u.branch_code = b.code " + searchQuery +  "and cv.created_date>=? and cv.created_date<=?" + " ORDER BY " + columnName + " " + columnSortOrder + " OFFSET " + start + "ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        results = this.jdbcKycTemplate.queryForList(mainSql, searchValue, fromDate, toDate);
                    } else {
                        mainSql = "select cv.id, concat(u.firstname, ' ', u.middlename, ' ', u.lastname) as champion, cv.name as agent_name, cv.msisdn as agent_phone, cv.location as agent_location, cv.captured_location, cv.captured_latitude, cv.captured_longitude, cv.activity_performed, cv.remarks_collected, cv.created_by, cv.created_date as visit_date, cv.account, cv.category, cv.signature, cv.location_photo, b.code, b.name from champion_visits_light cv inner join users u on cv.user_id = u.id inner join branches b on u.branch_code = b.code " + searchQuery  + "and cv.created_date>=? and cv.created_date<=? and u.branch_code=?" + " ORDER BY " + columnName + " " + columnSortOrder + " OFFSET " + start + "ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        results = this.jdbcKycTemplate.queryForList(mainSql, searchValue, fromDate, toDate, branch);
                    }
                } else {
                    searchQuery = " WHERE concat(cv.champion,' ',cv.agent_name,' ',cv.agent_location,' ',cv.agent_phone,' ',b.code,' ',b.name) LIKE ? and u.branch_code=? and cv.created_date>=? and cv.created_date<=? ";
                    totalRecordWithFilter = jdbcKycTemplate.queryForObject("SELECT count(id) FROM customers " + searchQuery, new Object[]{searchValue, branchCode, fromDate, toDate}, Integer.class);
                    mainSql = "select cv.id, concat(u.firstname, ' ', u.middlename, ' ', u.lastname) as champion, cv.name as agent_name, cv.msisdn as agent_phone, cv.location as agent_location, cv.captured_location, cv.captured_latitude, cv.captured_longitude, cv.activity_performed, cv.remarks_collected, cv.created_by, cv.created_date as visit_date, cv.account, cv.category, cv.signature, cv.location_photo, b.code, b.name from champion_visits_light cv inner join users u on cv.user_id = u.id inner join branches b on u.branch_code = b.code " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " OFFSET " + start + "ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                    results = this.jdbcKycTemplate.queryForList(mainSql, searchValue, branchCode, fromDate, toDate);
                }
            } else {
                totalRecordWithFilter = totalRecords;
                if (branchCode.equals("060")) {
                    if (branch == null || branch.isEmpty()) {
                        mainSql = "select cv.id, concat(u.firstname, ' ', u.middlename, ' ', u.lastname) as champion, cv.name as agent_name, cv.msisdn as agent_phone, cv.location as agent_location, cv.captured_location, cv.captured_latitude, cv.captured_longitude, cv.activity_performed, cv.remarks_collected, cv.created_by, cv.created_date as visit_date, cv.account, cv.category, cv.signature, cv.location_photo, b.code, b.name from champion_visits_light cv inner join users u on cv.user_id = u.id inner join branches b on u.branch_code = b.code where cv.created_date>=? and cv.created_date<=? ORDER BY " + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        results = this.jdbcKycTemplate.queryForList(mainSql, fromDate, toDate);
                    } else {
                        mainSql = "select cv.id, concat(u.firstname, ' ', u.middlename, ' ', u.lastname) as champion, cv.name as agent_name, cv.msisdn as agent_phone, cv.location as agent_location, cv.captured_location, cv.captured_latitude, cv.captured_longitude, cv.activity_performed, cv.remarks_collected, cv.created_by, cv.created_date as visit_date, cv.account, cv.category, cv.signature, cv.location_photo, b.code, b.name from champion_visits_light cv inner join users u on cv.user_id = u.id inner join branches b on u.branch_code = b.code where cv.created_date>=? and cv.created_date<=? and u.branch_code=? ORDER BY " + columnName + " " + columnSortOrder + " OFFSET " + start + "ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                        results = this.jdbcKycTemplate.queryForList(mainSql, fromDate, toDate, branch);
                    }
                } else {
                    mainSql = "select cv.id, concat(u.firstname, ' ', u.middlename, ' ', u.lastname) as champion, cv.name as agent_name, cv.msisdn as agent_phone, cv.location as agent_location, cv.captured_location, cv.captured_latitude, cv.captured_longitude, cv.activity_performed, cv.remarks_collected, cv.created_by, cv.created_date as visit_date, cv.account, cv.category, cv.signature, cv.location_photo, b.code, b.name from champion_visits_light cv inner join users u on cv.user_id = u.id inner join branches b on u.branch_code = b.code where cv.created_date>=? and cv.created_date<=? and u.branch_code=? ORDER BY " + columnName + " " + columnSortOrder + " OFFSET " + start + "ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                    results = this.jdbcKycTemplate.queryForList(mainSql, fromDate, toDate, branchCode);
                }
            }
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        return "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordWithFilter + "\",\"aaData\":" + jsonString + "}";
    }

    public String getWorkflowRoles(String roleId) {
        List<String> results = new ArrayList<>();
        try {
            String sql = "SELECT workflow_id FROM mofp_kyc_workflow WHERE role_id = ?";
            List<Map<String, Object>> workflowIds = this.jdbcTemplate.queryForList(sql, roleId);
            for (Map<String, Object> workflowId: workflowIds) {
                results.add((String) workflowId.get("workflow_id"));
            }
        } catch (DataAccessException e) {
            e.printStackTrace();
        }
        return String.join(",", results);
    }

    public String generateRandomAccount() {
        Random random = new Random();
        int randomNumber = random.nextInt(900) + 100; // Generate a 3-digit number (from 100 to 999)
        return "420400000" + randomNumber;
    }

    public String loadAgentRoles() {
        List<Map<String, Object>> results;
        try {
            results = jdbcKycTemplate.queryForList("SELECT id, name from roles");
            return this.jacksonMapper.writeValueAsString(results);
        } catch (DataAccessException e) {
            LOGGER.info(null, e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return "[]";
    }

    public String editAgentRole(String userId, String roleId) {
        int saveResult;
        try {
            saveResult = jdbcKycTemplate.update("UPDATE role_user ru set ru.role_id = ? WHERE ru.user_id = ?", roleId, userId);
            if (saveResult >= 1) {
                return "{\"success\": true, \"message\": \"Agent role updated successful!\"}";
            }
        } catch (DataAccessException e) {
            LOGGER.info(null, e);
        }
        return "{\"success\": false, \"message\": \"Could not update agent role, please try again!\"}";
    }

    public String getChampionVisitBlob(Long id, String col) throws JsonProcessingException {
        String sql;
        String result;
        if (Objects.equals(col, "signature")) {
            sql = "select cv.signature from champion_visits cv where cv.id = ?";
            String signature = this.jdbcKycTemplate.queryForObject(sql, String.class, id);
            result = "{\"signature\": \"" + signature + "\"}";
        } else if (Objects.equals(col, "location_photo")) {
            sql = "select cv.location_photo from champion_visits cv where cv.id = ?";
            String locationPhoto = this.jdbcKycTemplate.queryForObject(sql, String.class, id);
            result = "{\"location_photo\": \"" + locationPhoto + "\"}";
        } else {
            result = "{\"select\": \"1\"}";
        }
        LOGGER.info("result {}", result);
        return this.jacksonMapper.writeValueAsString(result);
    }
}
