/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.repository;

import com.DTO.Settings.ServiceProviderObj;
import com.DTO.Settings.ServiceProvidersForm;
import com.entities.Mail;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helper.DateUtil;
import com.service.CorebankingService;
import com.service.XMLParserService;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import philae.api.*;

import java.math.BigDecimal;
import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 *
 * @author MELLEJI
 */
@Repository
public class Settings_m {

    @Autowired
    @Qualifier("amgwdb")
    JdbcTemplate jdbcTemplate;
    @Autowired
    @Qualifier("jdbcCbsLive")
    JdbcTemplate jdbcCbsTemplate;
    @Autowired
    @Qualifier("gwBrinjalDbConnection")
    JdbcTemplate jdbcBrinjalTemplate;
    @Autowired
    ObjectMapper jacksonMapper;
    @Autowired
    CorebankingService coreBankingService;



    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Settings_m.class);

    public Mail getMails(String txnType) {
        try {
            return this.jdbcTemplate.queryForObject("select *  from escalation_matrix where mno=?", new Object[]{txnType},
                    (ResultSet rs, int rowNum) -> {
                        Mail mail = new Mail();
                        mail.setMailTo(rs.getString("mailTo"));
                        mail.setCc(rs.getString("cc"));

                        return mail;
                    });
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public Integer saveReconType(String txnType, String ttype) {
        return jdbcTemplate.update("insert into  recon_types(name,code) values(?,?)", txnType, ttype);
    }

    public Integer saveReconTypeReportsMapping(String reconId, String reportId, String displayName, String createdBy) {
        String sql = "INSERT INTO report_setup_txntype(report_setup_id, txntype_id,name,created_by) values(?,?,?,?)";//, reportId, reconId, displayName, createdBy);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setString(1, reportId);
            ps.setString(2, reconId);
            ps.setString(3, displayName);
            ps.setString(4, createdBy);
            return ps;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).intValue();
    }

    public List<Map<String, Object>> getReconTypes() {
        return this.jdbcTemplate.queryForList("SELECT * FROM recon_types");
    }

    /*
    get the recon components
     */
    public List<Map<String, Object>> getReconComponents(String reconTypeId) {
        return this.jdbcTemplate.queryForList("SELECT * FROM txns_types a join recon_types b on b.code=a.ttype where b.id=?", reconTypeId);
    }

    public List<Map<String, Object>> getBanksLists() {
        return this.jdbcTemplate.queryForList("select * from banks");
    }

    /*
    get the recon components by ID
     */
    public List<Map<String, Object>> getReconTypeById(String id) {
        return this.jdbcTemplate.queryForList("SELECT * FROM recon_types where id=?", id);
    }

    public List<Map<String, Object>> getReportsSetup(String reconType) {
        return this.jdbcTemplate.queryForList("SELECT * FROM report_setup where id not in (SELECT report_setup_id from report_setup_txntype where txntype_id=?)", reconType);
    }

    public int insertEmployee() {
        final String INSERT_SQL = "insert into trn_employee (first_name,last_name) values (?,?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(new PreparedStatementCreator() {
            public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                PreparedStatement ps = connection.prepareStatement(INSERT_SQL, new String[]{"employee_id"});
                ps.setString(1, "Yashwant");
                ps.setString(2, "Chavan");
                return ps;
            }
        }, keyHolder);

        return keyHolder.getKey().intValue();
    }

    //get reports setup per submitted recon type
    public List<Map<String, Object>> getMappedReports(String reconType) {
        return this.jdbcTemplate.queryForList("SELECT a.name displayName,b.name reportName,b.url reportUrl,c.name reconName,c.code ttype FROM report_setup_txntype a INNER JOIN report_setup b on b.id=a.report_setup_id INNER JOIN recon_types c on c.id=a.txntype_id WHERE c.id=?", reconType);
    }

    //get Domain controllers configured on the system
    public String getDomainControllers() {
        //Java objects to JSON string - compact-print - salamu - Pamoja.
        String jsonString = null;
        try {
            jsonString = this.jacksonMapper.writeValueAsString(this.jdbcTemplate.queryForList("SELECT * from domain_controller"));
            //LOGGER.info("RequestBody");
        } catch (JsonProcessingException ex) {
            LOGGER.info("RequestBody: ", ex);
        }
        return jsonString;
    }

    //get Domain controllers configured on the system
    public List<Map<String, Object>> getEscalationMatrix() {
        return this.jdbcTemplate.queryForList("SELECT * from escalation_matrix");
    }

    /*
    save recon Component
     */
    public int saveReconComponent(String name, String code, String ttype) {
        return jdbcTemplate.update("insert into txns_types(name,code,ttype,isAllowed) values(?,?,?,?)", name, code, ttype, 1);

    }

    public int saveServiceProvider(ServiceProvidersForm spForm, String createdBy) {
        int modulePermissionMapping = -1;
        try {
            modulePermissionMapping = this.jdbcTemplate.update("insert into service_providers(name,address,phone,bank_swiftcode,intermediary_bank,bank_account,facility_description,identifier,created_by) values(?,?,?,?,?,?,?,?,?)", new Object[]{spForm.getSpName(), spForm.getSpAddress(), spForm.getSpPhone(), spForm.getSpBankSwiftCode(), spForm.getIntermediaryBank(), spForm.getSpBankAccount(), spForm.getSpFacility(), spForm.getIdentifier(), createdBy});
        } catch (Exception e) {
            e.printStackTrace();
        }
        return modulePermissionMapping;
    }

    public int addTransferTimeExtension(String transferType, String extendedTime, String dayOfWeek, String modifiedBy) {
        int modulePermissionMapping = -1;
        try {
            modulePermissionMapping = this.jdbcTemplate.update("UPDATE transfer_calendar SET modified_dt=?,modified_by=?,cutt_off_time=? WHERE transfer_type=? and day_of_week=?", new Object[]{DateUtil.now(), modifiedBy, extendedTime, transferType, dayOfWeek.toLowerCase()});
        } catch (Exception e) {
            e.printStackTrace();
        }
        return modulePermissionMapping;
    }

    public int addNewBank(String bankName, String swiftCode, String identifier, String modifiedBy) {
        int modulePermissionMapping = -1;
        int defaultVal = -1;
        String fspCategory = "BANK";
        try {
            LOGGER.info("-----bank name:{},swiftCode:{},identifier:{}",bankName,swiftCode,identifier);
            modulePermissionMapping = this.jdbcTemplate.update("insert into banks(name,swift_code,created_by,identifier,swift_code_test,description,test_swift_code,tips_bank_code,fsp_category) VALUES(?,?,?,?,?,?,?,?,?) on duplicate key update swift_code=?", new Object[]{bankName, swiftCode, modifiedBy, identifier, swiftCode,defaultVal,defaultVal,defaultVal,defaultVal,fspCategory});
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.info("-----bank name:{},swiftCode:{},identifier:{}",bankName,swiftCode,identifier,e);

        }
        return modulePermissionMapping;
    }

    public List<Map<String, Object>> getBanks() {
        List<Map<String, Object>> results = null;
        try {
            results = this.jdbcTemplate.queryForList("SELECT id, name, swift_code, test_swift_code, identifier, description, created_by FROM banks order by name asc ");
        } catch (DataAccessException e) {
            LOGGER.info(null, e);
        }
        return results;
    }

    public String getServiceProvidersList(String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        try {
            String mainSql = "SELECT count(*) from service_providers";
            totalRecords = this.jdbcTemplate.queryForObject(mainSql, new Object[0], Integer.class);
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "'%" + searchValue + "%'";
                searchQuery = " WHERE  concat(name,' ',address,' ',phone,' ',bank_swiftcode,' ',bank_account,' ',facility_description,' ',identifier) LIKE ?";
                totalRecordwithFilter = this.jdbcTemplate.queryForObject("SELECT COUNT(*) FROM  service_providers" + searchQuery, new Object[]{searchValue}, Integer.class);
            } else {
                totalRecordwithFilter = totalRecords;
            }
            if (!searchQuery.equals("")) {
                mainSql = "SELECT * FROM  service_providers " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue});
            } else {
                mainSql = "SELECT * FROM  service_providers ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
//                LOGGER.info(mainSql);
                results = this.jdbcTemplate.queryForList(mainSql, new Object[0]);
            }
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        return "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
    }

    public List<Map<String, Object>> terminalRoles() {
        try {
            return jdbcBrinjalTemplate.queryForList("SELECT id, name from roles");
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            return null;
        }
    }


    public String listTerminals(String username, String locked, String blocked, String fromDate, String toDate,
                                String draw, String start, String rowPerPage, String searchValue, String columnIndex,
                                String columnName, String columnSortOrder) {
        List<Map<String, Object>> results;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        boolean isLocked = Boolean.parseBoolean(locked);
        boolean isBlocked = Boolean.parseBoolean(blocked);
        try {
            String mainSql = "SELECT count(*) from users";
            totalRecords = jdbcBrinjalTemplate.queryForObject(mainSql, new Object[0], Integer.class);
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                searchQuery = " WHERE concat(COALESCE(created_by,''),' ',COALESCE(deviceId,''),' ',COALESCE(email,''),' ',COALESCE(firstname,''),' ',COALESCE(lastname,''),' ',COALESCE(middlename,''),' ',COALESCE(username,''),' ',COALESCE(deviceName,''),' ',COALESCE(account,''),' ',COALESCE(location,'')) LIKE ?";
                if (isLocked)
                    searchQuery += " and (accountNoLocked = 0 or failedAttempt > 4)";
                else if (isBlocked)
                    searchQuery += " and enabled = 0";
                mainSql += searchQuery;
                totalRecordwithFilter = jdbcBrinjalTemplate.queryForObject(mainSql, new Object[]{searchValue}, Integer.class);
            } else {
                if (isLocked) {
                    mainSql = "SELECT count(*) from users where (accountNoLocked = 0 or failedAttempt > 4)";
                    totalRecords = jdbcBrinjalTemplate.queryForObject(mainSql, new Object[0], Integer.class);
                } else if (isBlocked) {
                    mainSql = "SELECT count(*) from users where enabled = 0";
                    totalRecords = jdbcBrinjalTemplate.queryForObject(mainSql, new Object[0], Integer.class);
                }
                totalRecordwithFilter = totalRecords;
            }
            LOGGER.info(mainSql);
            if (!searchQuery.equals("")) {
                mainSql = "SELECT * FROM users " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = jdbcBrinjalTemplate.queryForList(mainSql, new Object[]{searchValue});
            } else {
                if (isLocked)
                    mainSql = "SELECT * from users where (accountNoLocked = 0 or failedAttempt > 4) ORDER BY "
                            + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                else if (isBlocked)
                    mainSql = "SELECT * from users where enabled = 0 ORDER BY "
                            + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                else
                    mainSql = "SELECT * FROM users ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = jdbcBrinjalTemplate.queryForList(mainSql, new Object[0]);
            }
            LOGGER.info(mainSql);
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        return "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
    }

    public String getListTerminals(String locked, String blocked, String fromDate, String toDate,
                                String draw, String start, String rowPerPage, String searchValue, String columnIndex,
                                String columnName, String columnSortOrder) {
        List<Map<String, Object>> results;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        boolean isLocked = Boolean.parseBoolean(locked);
        boolean isBlocked = Boolean.parseBoolean(blocked);
        try {
            String mainSql = "SELECT count(*) from users";

            LOGGER.info("======>>>>>>mainSql:{}", mainSql);

            totalRecords = jdbcBrinjalTemplate.queryForObject(mainSql, new Object[0], Integer.class);
            String searchQuery = "";
            if (!searchValue.equals("")) {
                LOGGER.info("======with search");
                searchValue = "%" + searchValue + "%";
                searchQuery = " WHERE concat(COALESCE(created_by,''),' ',COALESCE(deviceId,''),' ',COALESCE(email,''),' ',COALESCE(firstname,''),' ',COALESCE(lastname,''),' ',COALESCE(middlename,''),' ',COALESCE(username,''),' ',COALESCE(deviceName,''),' ',COALESCE(account,''),' ',COALESCE(location,'')) LIKE ?";
                if (isLocked)
                    searchQuery += " and (accountNoLocked = 0 or failedAttempt > 4)";
                else if (isBlocked)
                    searchQuery += " and enabled = 0";
                mainSql += searchQuery;
                totalRecordwithFilter = jdbcBrinjalTemplate.queryForObject(mainSql, new Object[]{searchValue}, Integer.class);
            } else {
                LOGGER.info("======empty search");
                if (isLocked) {
                    mainSql = "SELECT count(*) from users where (accountNoLocked = 0 or failedAttempt > 4)";
                    totalRecords = jdbcBrinjalTemplate.queryForObject(mainSql, new Object[0], Integer.class);
                } else if (isBlocked) {
                    mainSql = "SELECT count(*) from users where enabled = 0";
                    totalRecords = jdbcBrinjalTemplate.queryForObject(mainSql, new Object[0], Integer.class);
                }
                totalRecordwithFilter = totalRecords;
            }
            LOGGER.info("===>>> here {}" + mainSql);

            if (!searchQuery.equals("")) {
                LOGGER.info("======with search 1");
                mainSql = "SELECT * FROM users " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = jdbcBrinjalTemplate.queryForList(mainSql, new Object[]{searchValue});
            } else {
                if (isLocked)
                    mainSql = "SELECT * from users where (accountNoLocked = 0 or failedAttempt > 4) ORDER BY "
                            + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                else if (isBlocked)
                    mainSql = "SELECT * from users where enabled = 0 ORDER BY "
                            + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                else
                    LOGGER.info("======else conditions 1");
                mainSql = "SELECT * FROM users ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                LOGGER.info("=======<<<>>> truely {}", mainSql);
                results = jdbcBrinjalTemplate.queryForList(mainSql, new Object[0]);
            }
            LOGGER.info(mainSql);
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        return "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
    }

    public AqResponse getTerminalAccount(String account) {
        AqResponse queryDepositAccountResponse = null;
        try {
            String xml =
                    "<request>\n" +
                            "   <reference>" + DateUtil.now("yyyyMMddHHmmss") +  "</reference>\n" +
                            "   <!--Optional:-->\n" +
                            "   <accountNumber>" + account + "</accountNumber>\n" +
                            "</request>\n";
            queryDepositAccountResponse = XMLParserService.jaxbXMLToObject(
                    coreBankingService.processRequestToCore(xml, "api:queryDepositAccount"), AqResponse.class);
            if (queryDepositAccountResponse == null) {
                throw new Exception("Sorry, no network connection... Please try again later!");
            }
        } catch (Exception e) {
           throw new RuntimeException(e);
        }
        return queryDepositAccountResponse;
    }

    public String getMobileNumber(Long customerId) {
        return this.coreBankingService.mobileNumberQuery(customerId);
    }

//    @Transactional
    public int saveTerminal(String sysUser, String phone, String terminalId, String role, AqResponse queryDepositAccountResponse) {
        CnAccount account = queryDepositAccountResponse.getAccount();
        String accountName = account.getAccountName();
        String accountNumber = account.getAccountNumber();
        String accountType = account.getAccountType();
        Long accountId= account.getAcctId();
        String customerCat = account.getCustCat();
        Long customerId= account.getCustId();
        Long productId= account.getProductId();
        String accountStatus= account.getStatus();
        BigDecimal clearedBalance= account.getClearedBalance();
        CnBranch branch = account.getBranch();
        Long buId = branch.getBuId();
        String buCode = branch.getBuCode();
        String buName = branch.getBuName();
        String glPrefix = branch.getGlPrefix();
        String branchStatus = branch.getStatus();
        CnCurrency currency = account.getCurrency();
        Long currencyId = currency.getId();
        String currencyCode = currency.getCode();
        String currencyName = currency.getName();
        Integer points = currency.getPoints();
        XaResponse saveTerminalsResponse;
        String firstName = ""; String middleName = ""; String lastName = "";
        String[] names = accountName.split(" ");
        if (names.length == 3) {
            firstName = names[0];
            middleName = names[1];
            lastName = names[2];
        } else if (names.length == 2) {
            firstName = names[0];
            middleName = "";
            lastName = names[1];
        }
        int userId;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        String finalFirstName = firstName;
        String finalMiddleName = middleName;
        String finalLastName = lastName;
        jdbcBrinjalTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("insert into users (created_by, created_date, rec_status," +
                    " accountNoExpired, accountNoLocked, credentialsNoExpired, deviceId, deviceName, email, enabled, failedAttempt," +
                    " firstLogin, firstname, middlename, lastname, password, phone, username, branchCode, noOfSkippedAppUpdates," +
                    " appVersion, account, location) VALUES(?,?,?,CAST(? AS UNSIGNED),CAST(? AS UNSIGNED),CAST(? AS UNSIGNED)," +
                    "?,?,?,CAST(? AS UNSIGNED),?,?,?,?,?,?,?,?,?,?,?,?,?) on duplicate key update username=?", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, sysUser);
            ps.setString(2, DateUtil.now());
            ps.setString(3, "A");
            ps.setString(4, "1");
            ps.setString(5, "1");
            ps.setString(6, "1");
            ps.setString(7, null);
            ps.setString(8, accountName);
            ps.setString(9, null);
            ps.setString(10, "1");
            ps.setInt(11, 0);
            ps.setString(12, "Y");
            ps.setString(13, finalFirstName);
            ps.setString(14, finalMiddleName);
            ps.setString(15, finalLastName);
            ps.setString(16, new BCryptPasswordEncoder().encode("0000"));
            ps.setString(17, phone);
            ps.setString(18, terminalId);
            ps.setString(19, buCode);
            ps.setInt(20, 0);
            ps.setString(21, "6");
            ps.setString(22, accountNumber);
            ps.setString(23, buName);
            ps.setString(24, terminalId);
            return ps;
        }, keyHolder);
        if (keyHolder.getKey() != null) {
            userId = Objects.requireNonNull(keyHolder.getKey()).intValue();
        } else {
            throw new DataIntegrityViolationException("Terminal already exists");
        }
        jdbcBrinjalTemplate.update("insert into role_user(user_id,role_id) values(?,?)", userId, role);
        try {
            String xml =
                    "<request>\n" +
                    "   <reference>" + DateUtil.now("yyyyMMddHHmmss") +  "</reference>\n" +
                    "   <!--Optional:-->\n" +
                    "   <terminal>\n" +
                    "      <!--Optional:-->\n" +
                    "      <buCode>" + buCode + "</buCode>\n" +
                    "      <!--Optional:-->\n" +
                    "      <channel>POS</channel>\n" +
                    "      <!--Optional:-->\n" +
                    "      <location>" + buName + "</location>\n" +
                    "      <!--Optional:-->\n" +
                    "      <operator>A</operator>\n" +
                    "      <!--Optional:-->\n" +
                    "      <scheme>P01</scheme>\n" +
                    "      <!--Optional:-->\n" +
                    "      <status>A</status>\n" +
                    "      <!--Optional:-->\n" +
                    "      <sysDate>" + DateUtil.dateToGregorianCalendar(DateUtil.now(), "yyyy-MM-dd HH:mm:ss") +  "</sysDate>\n" +
                    "      <!--Optional:-->\n" +
                    "      <sysUser>" + sysUser + "</sysUser>\n" +
                    "      <!--Optional:-->\n" +
                    "      <terminalId>" + terminalId + "</terminalId>\n" +
                    "   </terminal>\n" +
                    "   <!--Zero or more repetitions:-->\n" +
                    "   <accounts>\n" +
                    "      <!--Optional:-->\n" +
                    "      <accountName>" + accountName + "</accountName>\n" +
                    "      <!--Optional:-->\n" +
                    "      <accountNumber>" + accountNumber + "</accountNumber>\n" +
                    "      <!--Optional:-->\n" +
                    "      <accountType>" + accountType + "</accountType>\n" +
                    "      <!--Optional:-->\n" +
                    "      <acctId>" + accountId + "</acctId>\n" +
                    "      <!--Optional:-->\n" +
                    "      <branch>\n" +
                    "         <!--Optional:-->\n" +
                    "         <buCode>" + buCode + "</buCode>\n" +
                    "         <!--Optional:-->\n" +
                    "         <buId>" + buId + "</buId>\n" +
                    "         <!--Optional:-->\n" +
                    "         <buName>" + buName + "</buName>\n" +
                    "         <!--Optional:-->\n" +
                    "         <glPrefix>" + glPrefix + "</glPrefix>\n" +
                    "         <!--Optional:-->\n" +
                    "         <status>" + branchStatus + "</status>\n" +
                    "      </branch>\n" +
                    "      <!--Optional:-->\n" +
                    "      <clearedBalance>" + clearedBalance + "</clearedBalance>\n" +
                    "      <!--Optional:-->\n" +
                    "      <currency>\n" +
                    "         <!--Optional:-->\n" +
                    "         <code>" + currencyCode + "</code>\n" +
                    "         <!--Optional:-->\n" +
                    "         <id>" + currencyId + "</id>\n" +
                    "         <!--Optional:-->\n" +
                    "         <name>" + currencyName + "</name>\n" +
                    "         <!--Optional:-->\n" +
                    "         <points>" + points + "</points>\n" +
                    "      </currency>\n" +
                    "      <!--Optional:-->\n" +
                    "      <custCat>" + customerCat + "</custCat>\n" +
                    "      <!--Optional:-->\n" +
                    "      <custId>" + customerId + "</custId>\n" +
                    "      <!--Optional:-->\n" +
                    "      <productId>" + productId + "</productId>\n" +
                    "      <!--Optional:-->\n" +
                    "      <shortName>" + firstName + " " + lastName + "</shortName>\n" +
                    "      <!--Optional:-->\n" +
                    "      <status>" + accountStatus + "</status>\n" +
                    "   </accounts>\n" +
                    "</request>";
            saveTerminalsResponse = XMLParserService.jaxbXMLToObject(
                    coreBankingService.processRequestToCore(xml, "api:saveTerminal"), XaResponse.class);
            int responseCode = -1;
            if (saveTerminalsResponse != null) {
                responseCode = saveTerminalsResponse.getResult();
            }
            return responseCode;
        } catch (Exception e) {
            LOGGER.info(null, e);
        }
        return -1;
    }

    public int editTerminal(String sysUser, String id, String terminalId, String firstname, String middlename,
                            String lastname, String phone, String email, String appVersion, String tin, String region,
                            String district, String ward, String town, String location, String latitude, String longitude,
                            AqResponse queryDepositAccountResponse) {
        CnAccount account = queryDepositAccountResponse.getAccount();
        String accountName = account.getAccountName();
        String accountNumber = account.getAccountNumber();
        String accountType = account.getAccountType();
        Long accountId= account.getAcctId();
        String customerCat = account.getCustCat();
        Long customerId= account.getCustId();
        Long productId= account.getProductId();
        String accountStatus= account.getStatus();
        BigDecimal clearedBalance= account.getClearedBalance();
        CnBranch branch = account.getBranch();
        Long buId = branch.getBuId();
        String buCode = branch.getBuCode();
        String buName = branch.getBuName();
        String glPrefix = branch.getGlPrefix();
        String branchStatus = branch.getStatus();
        CnCurrency currency = account.getCurrency();
        Long currencyId = currency.getId();
        String currencyCode = currency.getCode();
        String currencyName = currency.getName();
        Integer points = currency.getPoints();
        XaResponse saveTerminalsResponse;
        int result = -1;
        try {
            result = jdbcBrinjalTemplate.update("update users set last_modified_by = ?, last_modified_date = ?, email = ?, firstname = ?, " +
                    "middlename = ?, lastname = ?, phone = ?, username = ?, appVersion = ?, account = ?, location = ?, " +
                    "deviceName = ?, tin = ?, region = ?, district = ?, ward = ?, town = ?, latitude = ?, longitude = ? " +
                    "where id = ?", new Object[]{sysUser, DateUtil.now(), email, firstname, middlename,
                    lastname, phone, terminalId, appVersion, accountNumber, location, accountName, tin, region, district,
                    ward, town, latitude, longitude, id});
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (result >= 1)
            try {
                String xml =
                        "<request>\n" +
                                "   <reference>" + DateUtil.now("yyyyMMddHHmmss") +  "</reference>\n" +
                                "   <!--Optional:-->\n" +
                                "   <terminal>\n" +
                                "      <!--Optional:-->\n" +
                                "      <buCode>" + buCode + "</buCode>\n" +
                                "      <!--Optional:-->\n" +
                                "      <channel>POS</channel>\n" +
                                "      <!--Optional:-->\n" +
                                "      <location>" + buName + "</location>\n" +
                                "      <!--Optional:-->\n" +
                                "      <operator>A</operator>\n" +
                                "      <!--Optional:-->\n" +
                                "      <scheme>P01</scheme>\n" +
                                "      <!--Optional:-->\n" +
                                "      <status>A</status>\n" +
                                "      <!--Optional:-->\n" +
                                "      <sysDate>" + DateUtil.dateToGregorianCalendar(DateUtil.now(), "yyyy-MM-dd HH:mm:ss") +  "</sysDate>\n" +
                                "      <!--Optional:-->\n" +
                                "      <sysUser>" + sysUser + "</sysUser>\n" +
                                "      <!--Optional:-->\n" +
                                "      <terminalId>" + terminalId + "</terminalId>\n" +
                                "   </terminal>\n" +
                                "   <!--Zero or more repetitions:-->\n" +
                                "   <accounts>\n" +
                                "      <!--Optional:-->\n" +
                                "      <accountName>" + accountName + "</accountName>\n" +
                                "      <!--Optional:-->\n" +
                                "      <accountNumber>" + accountNumber + "</accountNumber>\n" +
                                "      <!--Optional:-->\n" +
                                "      <accountType>" + accountType + "</accountType>\n" +
                                "      <!--Optional:-->\n" +
                                "      <acctId>" + accountId + "</acctId>\n" +
                                "      <!--Optional:-->\n" +
                                "      <branch>\n" +
                                "         <!--Optional:-->\n" +
                                "         <buCode>" + buCode + "</buCode>\n" +
                                "         <!--Optional:-->\n" +
                                "         <buId>" + buId + "</buId>\n" +
                                "         <!--Optional:-->\n" +
                                "         <buName>" + buName + "</buName>\n" +
                                "         <!--Optional:-->\n" +
                                "         <glPrefix>" + glPrefix + "</glPrefix>\n" +
                                "         <!--Optional:-->\n" +
                                "         <status>" + branchStatus + "</status>\n" +
                                "      </branch>\n" +
                                "      <!--Optional:-->\n" +
                                "      <clearedBalance>" + clearedBalance + "</clearedBalance>\n" +
                                "      <!--Optional:-->\n" +
                                "      <currency>\n" +
                                "         <!--Optional:-->\n" +
                                "         <code>" + currencyCode + "</code>\n" +
                                "         <!--Optional:-->\n" +
                                "         <id>" + currencyId + "</id>\n" +
                                "         <!--Optional:-->\n" +
                                "         <name>" + currencyName + "</name>\n" +
                                "         <!--Optional:-->\n" +
                                "         <points>" + points + "</points>\n" +
                                "      </currency>\n" +
                                "      <!--Optional:-->\n" +
                                "      <custCat>" + customerCat + "</custCat>\n" +
                                "      <!--Optional:-->\n" +
                                "      <custId>" + customerId + "</custId>\n" +
                                "      <!--Optional:-->\n" +
                                "      <productId>" + productId + "</productId>\n" +
                                "      <!--Optional:-->\n" +
                                "      <shortName>" + firstname + " " + lastname + "</shortName>\n" +
                                "      <!--Optional:-->\n" +
                                "      <status>" + accountStatus + "</status>\n" +
                                "   </accounts>\n" +
                                "</request>";
                saveTerminalsResponse = XMLParserService.jaxbXMLToObject(
                        coreBankingService.processRequestToCore(xml, "api:saveTerminal"), XaResponse.class);
                int responseCode = -1;
                if (saveTerminalsResponse != null) {
                    responseCode = saveTerminalsResponse.getResult();
                }
                return responseCode;
            } catch (Exception e) {
                LOGGER.info(null, e);
            }
        return result;
    }

    public String unlockTerminal(String id, String username) {
        int result = -1;
        try {
            result = jdbcBrinjalTemplate.update("UPDATE users SET accountNoLocked=?,failedAttempt=?,last_modified_by=?," +
                    "last_modified_date=? WHERE id=? ", new Object[]{ 1, 0, username, DateUtil.now(), id});
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (result >= 1)
            return "{\"success\": true, \"message\": \"Terminal unlocked successful!\"}";
        else
            return "{\"success\": false, \"message\": \"Could not unlock terminal, please try again!\"}";
    }

    public String resetPassword(String id, String username) {
        int result = -1;
        try {
            result = jdbcBrinjalTemplate.update("UPDATE users SET password=?, last_modified_by=?, last_modified_date=? WHERE id=? ",
                    new Object[]{ new BCryptPasswordEncoder().encode("0000"), username, DateUtil.now(), id});
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (result >= 1)
            return "{\"success\": true, \"message\": \"Terminal password reset successful!\"}";
        else
            return "{\"success\": false, \"message\": \"Could not reset password terminal, please try again!\"}";
    }

    public String blockTerminal(String id, String remarks, String username) {
        int result = -1;
        try {
            result = jdbcBrinjalTemplate.update("UPDATE users SET enabled=?, last_modified_by=?, " +
                    "last_modified_date=?, remarks=? WHERE id=? ", new Object[]{ 0, username, DateUtil.now(), id, remarks});
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (result >= 1)
            return "{\"success\": true, \"message\": \"Terminal blocked successful!\"}";
        else
            return "{\"success\": false, \"message\": \"Could not block terminal, please try again!\"}";
    }

    public String unblockTerminal(String id, String username) {
        int result = -1;
        try {
            result = jdbcBrinjalTemplate.update("UPDATE users SET enabled=?, last_modified_by=?, remarks=null, " +
                    "last_modified_date=? WHERE id=? ", new Object[]{ 1, username, DateUtil.now(), id});
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (result >= 1)
            return "{\"success\": true, \"message\": \"Terminal unblocked successful!\"}";
        else
            return "{\"success\": false, \"message\": \"Could not unblock terminal, please try again!\"}";
    }

    public String getTerminalTransactions(String draw, String start, String rowPerPage, String searchValue,
                                          String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        try {
            String mainSql = "SELECT count(*) from transactions WHERE channel = 'POS'";
            totalRecords = jdbcBrinjalTemplate.queryForObject(mainSql, new Object[0], Integer.class);
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                searchQuery = " WHERE channel = 'POS' AND concat(COALESCE(txn_type,''),' ',COALESCE(sourceAcct,''),' ',COALESCE(destinationAcct,''),' ',COALESCE(beneficiaryName,''),' ',COALESCE(amount,''),' ',COALESCE(reference,''),' ',COALESCE(status,''),' ',COALESCE(cbs_remarks,''),' ',COALESCE(device_id,''),' ',COALESCE(msisdn,''),' ',COALESCE(receipt,'')) LIKE ?";
                totalRecordwithFilter = jdbcBrinjalTemplate.queryForObject("SELECT COUNT(*) FROM transactions" + searchQuery, new Object[]{searchValue}, Integer.class);
            } else {
                totalRecordwithFilter = totalRecords;
            }
            if (!searchQuery.equals("")) {
                mainSql = "SELECT * FROM transactions " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = jdbcBrinjalTemplate.queryForList(mainSql, new Object[]{searchValue});
            } else {
                mainSql = "SELECT * FROM transactions WHERE channel = 'POS' ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = jdbcBrinjalTemplate.queryForList(mainSql, new Object[0]);
            }
            LOGGER.info(mainSql);
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        return "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
    }

    public String getLoginStatus() {
        List<Map<String, Object>> results;
        String jsonString = null;
        try {
            String mainSql = "select firstLogin, count(*) count from users group by firstLogin order by firstLogin desc";
            results = jdbcBrinjalTemplate.queryForList(mainSql);
            LOGGER.info(mainSql);
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        return jsonString;
    }

    public String getLockedStatus() {
        List<Map<String, Object>> results;
        String jsonString = null;
        try {
            String mainSql = "select accountNoLocked, count(*) count from users group by accountNoLocked order by accountNoLocked desc";
            results = jdbcBrinjalTemplate.queryForList(mainSql);
            LOGGER.info(mainSql);
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        return jsonString;
    }

    public String getBlockedStatus() {
        List<Map<String, Object>> results;
        String jsonString = null;
        try {
            String mainSql = "select enabled, count(*) count from users group by enabled order by enabled desc";
            results = jdbcBrinjalTemplate.queryForList(mainSql);
            LOGGER.info(mainSql);
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        return jsonString;
    }

    public String getUtilitiesReport(String paramString) {
        if (paramString == null || paramString.isEmpty())
            paramString = "2022-01-01 00:00:59";
        List<Map<String, Object>> results;
        String jsonString = null;
        try {
            String mainSql = "SELECT if(txn_type in ('VODA-AIRTIME', 'TIGO-AIRTIME', 'AIRTIME-AIRTEL'," +
                    " 'HALOTEL-AIRTIME', 'ZANTEL-AIRTIME'), 'AIRTIME', if(txn_type in ('AZAM', 'DSTV'), 'TV', txn_type))" +
                    " as category, count(id) count, sum(amount) total FROM transactions WHERE channel_id = '01'" +
                    " AND txn_type in ('LUKU', 'GePG-PAYMENTS', 'VODA-AIRTIME', 'TIGO-AIRTIME', 'AIRTIME-AIRTEL'," +
                    " 'HALOTEL-AIRTIME', 'ZANTEL-AIRTIME', 'AZAM', 'DSTV') AND create_date >= ? group by category";
            results = jdbcBrinjalTemplate.queryForList(mainSql, new Object[] { paramString });
            LOGGER.info(mainSql);
            jsonString = this.jacksonMapper.writeValueAsString(results);
            LOGGER.info(jsonString);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        return jsonString;
    }

    public String getAirtimeReport(String paramString) {
        if (paramString == null || paramString.isEmpty())
            paramString = "2022-01-01 00:00:59";
        List<Map<String, Object>> results;
        String jsonString = null;
        try {
            String mainSql = "SELECT txn_type, count(id) count, sum(amount) total FROM transactions WHERE channel_id = '01'" +
                    " AND txn_type in ('VODA-AIRTIME', 'TIGO-AIRTIME', 'AIRTIME-AIRTEL', 'HALOTEL-AIRTIME', 'ZANTEL-AIRTIME')" +
                    " AND create_date >= ? group by txn_type";
            results = jdbcBrinjalTemplate.queryForList(mainSql, new Object[] { paramString });
            LOGGER.info(mainSql);
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        return jsonString;
    }

    public String getTVReport(String paramString) {
        if (paramString == null || paramString.isEmpty())
            paramString = "2022-01-01 00:00:59";
        List<Map<String, Object>> results;
        String jsonString = null;
        try {
            String mainSql = "SELECT txn_type, count(id) count, sum(amount) total FROM transactions WHERE" +
                    " channel_id = '01' AND txn_type in ('AZAM', 'DSTV', 'STARTIMES') AND create_date >= ? group by txn_type";
            results = jdbcBrinjalTemplate.queryForList(mainSql, new Object[] { paramString });
            LOGGER.info(mainSql);
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        return jsonString;
    }

    public List<Map<String, Object>> getTransactions(String paramString) {
        if (paramString == null || paramString.isEmpty())
            paramString = "2022-01-01 00:00:59";
        try {
            String str = "select ttd.name name, ttd.image_url, COALESCE(t.count, 0) count," +
                    " COALESCE(sum(t.total), 0) total from (SELECT count(id) count, sum(amount) total," +
                    " if(txn_type in ('LUKU', 'GePG-PAYMENTS', 'VODA-AIRTIME', 'TIGO-AIRTIME'," +
                    " 'AIRTIME-AIRTEL', 'HALOTEL-AIRTIME', 'ZANTEL-AIRTIME', 'AZAM', 'DSTV')," +
                    " 'UTILITIES', txn_type) as category FROM transactions WHERE channel_id = '01'" +
                    " AND create_date >= ? group by category) t right join txn_type_defn ttd on" +
                    " t.category = ttd.name group by ttd.name";
            return jdbcBrinjalTemplate.queryForList(str, new Object[] { paramString });
        } catch (Exception exception) {
            LOGGER.info(null, exception);
            return null;
        }
    }

    public String getTransactionAjax(String paramString) {
        String str = null;
        if (paramString == null || paramString.isEmpty())
            paramString = "2022-01-01 00:00:59";
        try {
            String str1 = "select ttd.name name, ttd.image_url, COALESCE(t.count, 0) count," +
                    " COALESCE(sum(t.total), 0) total from (SELECT count(id) count, sum(amount) total," +
                    " if(txn_type in ('LUKU', 'GePG-PAYMENTS', 'VODA-AIRTIME', 'TIGO-AIRTIME'," +
                    " 'AIRTIME-AIRTEL', 'HALOTEL-AIRTIME', 'ZANTEL-AIRTIME', 'AZAM', 'DSTV')," +
                    " 'UTILITIES', txn_type) as category FROM transactions WHERE channel_id = '01'" +
                    " AND create_date >= ? group by category) t right join txn_type_defn ttd on" +
                    " t.category = ttd.name group by ttd.name";
            LOGGER.info(str1);
            List<Map<String, Object>> list = jdbcBrinjalTemplate.queryForList(str1, new Object[] { paramString });
            str = this.jacksonMapper.writeValueAsString(list);
            LOGGER.info(str);
        } catch (Exception exception) {
            exception.printStackTrace();
            LOGGER.error("RequestBody: ", exception);
        }
        return str;
    }

    public String loadAppVersions() {
        String response = null;
        try {
            String query = "select versionName, versionNo from appversions where isAllowed = '1'";
            List<Map<String, Object>> list = jdbcBrinjalTemplate.queryForList(query);
            response = this.jacksonMapper.writeValueAsString(list);
            LOGGER.info(response);
        } catch (Exception exception) {
            exception.printStackTrace();
            LOGGER.error("RequestBody: ", exception);
        }
        return response;
    }

    public String loadRegions() {
        List<Map<String, Object>> results;
        try {
            results = this.jdbcTemplate.queryForList("SELECT id, region FROM regions order by id asc ");
            return this.jacksonMapper.writeValueAsString(results);
        } catch (DataAccessException e) {
            LOGGER.info(null, e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return "";
    }

    public String loadDistrictsByRegion(String region) {
        List<Map<String, Object>> results;
        try {
            results = this.jdbcTemplate.queryForList("SELECT id, district FROM districts where region_id = ? order by id asc ",
                    region);
            return this.jacksonMapper.writeValueAsString(results);
        } catch (DataAccessException e) {
            LOGGER.info(null, e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return "";
    }

    public ServiceProviderObj getSProviderData(String sproviderId) {
       ServiceProviderObj spo = jdbcTemplate.queryForObject("SELECT * FROM service_providers WHERE id=?",new Object[]{sproviderId},(ResultSet rs, int rowNum)->{
           ServiceProviderObj spob = new ServiceProviderObj();
           spob.setId(Integer.parseInt(rs.getString("id")));
           spob.setName(rs.getString("name"));
           spob.setAddress(rs.getString("address"));
           spob.setPhone(rs.getString("phone"));
           spob.setBankSwiftCode(rs.getString("bank_swiftcode"));
           spob.setIntermediaryBank(rs.getString("intermediary_bank"));
           spob.setBankAccount(rs.getString("bank_account"));
           spob.setFacilityDescription(rs.getString("facility_description"));
           spob.setIdentifier(rs.getString("identifier"));
           return spob;
       });
       return spo;
    }


    public int updateServiceProvider(ServiceProvidersForm customequery,String spId, String username) {
        int saveResult = 0;
        String sql ="UPDATE service_providers SET name=?,address=?,phone=?,bank_swiftcode=?,bank_account=?,facility_description=?,identifier=? WHERE id=?";
        try {
//            LOGGER.info(sql.replace("?","'{}'"),  customequery.getSpName(),customequery.getSpAddress(), customequery.getSpPhone(), customequery.getSpBankSwiftCode(), customequery.getSpBankAccount(),customequery.getSpFacility(), customequery.getSpBankSwiftCode().split("==")[1], spId);
            saveResult = jdbcTemplate.update(sql, customequery.getSpName(),customequery.getSpAddress(), customequery.getSpPhone(), customequery.getSpBankSwiftCode(), customequery.getSpBankAccount(),customequery.getSpFacility(), customequery.getSpBankSwiftCode().split("==")[1], spId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return saveResult;
    }

    public String searchTerminalNo(String terminalNo) {
        List<Map<String, Object>> results;
        try {
            results = jdbcBrinjalTemplate.queryForList("SELECT * from users WHERE username=? and accountNoLocked=1 and enabled=1",
                    terminalNo);
            LOGGER.info("SELECT * from users WHERE username=? and accountNoLocked=1 and enabled=1".replace("?","'{}'"), terminalNo);
            LOGGER.info("Users response {}", results);
            Map<String, Object> map = results.get(0);
            LOGGER.info("Map response {}", map);
            return this.jacksonMapper.writeValueAsString(map);
        } catch (DataAccessException e) {
            LOGGER.info(null, e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return "[]";
    }

    public String loadTerminalRoles() {
        List<Map<String, Object>> results;
        try {
            results = jdbcBrinjalTemplate.queryForList("SELECT id, name from roles");
            return this.jacksonMapper.writeValueAsString(results);
        } catch (DataAccessException e) {
            LOGGER.info(null, e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return "[]";
    }

    public String editTerminalRole(String terminalId, String roleId) {
        int saveResult;
        try {
            saveResult = jdbcBrinjalTemplate.update("UPDATE role_user ru set ru.role_id = ? WHERE ru.user_id = ?", roleId, terminalId);
            if (saveResult >= 1) {
                return "{\"success\": true, \"message\": \"Terminal role updated successful!\"}";
            }
        } catch (DataAccessException e) {
            LOGGER.info(null, e);
        }
        return "{\"success\": false, \"message\": \"Could not update terminal role, please try again!\"}";
    }
}
