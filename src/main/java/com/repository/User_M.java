/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.repository;

import com.core.event.AuditLogEvent;
import com.entities.Modules;
import com.entities.UserForm;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helper.DateUtil;
import com.repository.reports.ReconReportsRepo;
import com.service.JasperService;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletResponse;
import net.sf.jasperreports.engine.JasperPrint;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author MELLEJI
 */
@Repository
public class User_M {

    @Autowired
    @Qualifier("amgwdb")
    JdbcTemplate jdbcTemplate;

    @Autowired
    ObjectMapper jacksonMapper;

    @Autowired
    JasperService jasperService;

    @Autowired
    @Qualifier("amgwConnection")
    HikariDataSource cilantroDataSource;

//    @Autowired
//    SecurityConfig userSecurity;
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(User_M.class);

    public List<Modules> getModules(String userId, String roleId) {
        try {
            String sql = "select a.id,a.ti_icon,a.name,a.url from modules a INNER join module_roles b on b.module_id=a.id WHERE b.role_id in  (select a.role_id from user_roles a inner join roles b on a.role_id=b.id where a.user_id = ?) and b.role_id=?";
            LOGGER.info(sql.replace("?", "'{}'"), userId, roleId);
            List<Modules> modules
                    = this.jdbcTemplate.query(sql, new Object[]{userId, roleId},
                            (ResultSet rs, int rowNum) -> {
                                Modules module = new Modules();
                                module.setId(rs.getString("id"));
                                module.setIcon(rs.getString("ti_icon"));
                                module.setName(rs.getString("name"));
                                module.setUrl(rs.getString("url"));
                                module.setGetSubPermissions(getSubModulesPermissions());
                                module.setGetOperationsPermissions(getOperationsPermission(roleId));
                                module.setGetPermissions(getUserPermissions(userId, rs.getString("id"), roleId));
                                module.setPaymentsModules(getUserPaymentsModulesPerRole(roleId));
                                module.setPaymentsPermissions(getUserPaymentsModulesPermissionPerRole(roleId));
                                module.setAuthorities(grantedAuthorities(roleId));
                                module.setKycPermissions(getKycModulesPermissionsPerRole(roleId));
                                //LOGGER.info("SINGLE MODULE:{}", module.toString());
                                return module;
                            });
            return modules;
        } catch (EmptyResultDataAccessException e) {
            LOGGER.info(null, e);
            return null;
        }
    }

    public List<Map<String, Object>> getUserPermissions(String userId, String moduleId, String roleId) {
        String sql = "select a.name,a.url,a.sub_url,a.url2 from permissions a INNER JOIN module_permission b on a.id=b.permission_id INNER join module_roles c on c.module_id=b.module_id where a.display_status='A' and b.module_id=? and a.id in (select bb.permission_id from permission_role  bb  INNER join  user_roles d ON d.role_id=bb.role_id  INNER JOIN users aa on aa.id=d.user_id where aa.id=?) and c.role_id=?";
        LOGGER.info("SUBMODULES:");
        LOGGER.info(sql.replace("?", "'{}'"), moduleId, userId, roleId);
        return this.jdbcTemplate.queryForList(sql, moduleId, userId, roleId);
    }

    public List<Map<String, Object>> getGeneralUserPermissions(String userId, String moduleId, String roleId) {
        return this.jdbcTemplate.queryForList("select * from permission_role pr join permissions p on pr.permission_id =p.id where pr.role_id =?", roleId);
    }

    private List<GrantedAuthority> grantedAuthorities(String roleId) {
        List<GrantedAuthority> grantedAuths = new ArrayList<>();
        try {
            List<Map<String, Object>> perms = this.jdbcTemplate.queryForList("select * from static_permission_role pr join permissions p on pr.permission_id =p.id where pr.role_id =?", roleId);

            perms.forEach((grantedAuth) -> {
                grantedAuths.add(new SimpleGrantedAuthority(grantedAuth.get("url").toString()));
            });
        } catch (DataAccessException e) {
            LOGGER.info("DataAccessException", e);
        }
        return grantedAuths;
    }

    /*
    GET PAYMENTS MODULES PERMISSIONS
     */
    public List<Map<String, Object>> getUserPaymentsModulesPerRole(String roleId) {
        List<Map<String, Object>> result = null;
        try {
            result = this.jdbcTemplate.queryForList("SELECT a.name,a.image_url,a.module_dashboard_url FROM payment_modules a INNER JOIN payment_module_permission b on b.module_id=a.id INNER join payment_permission_role c on c.payment_permission_id=b.permission_id where c.role_id=? GROUP by a.id", roleId);
        } catch (Exception ex) {
            LOGGER.info(null, ex);
        }
        return result;
    }

    /*
    GET PAYMENTS MODULES PERMISSIONS
     */
    public List<Map<String, Object>> getUserPaymentsModulesPermissionPerRole(String roleId) {
        List<Map<String, Object>> result = null;
        try {
            result = this.jdbcTemplate.queryForList("SELECT d.name,d.url,d.ajax_url FROM payment_permission_role c INNER JOIN payment_permissions d on d.id=c.payment_permission_id where c.role_id=?", roleId);
        } catch (Exception ex) {
            LOGGER.info(null, ex);
        }
        return result;
    }

    public List<Map<String, Object>> getUserRole(String username) {
        List<Map<String, Object>> result = null;
        try {
            result = this.jdbcTemplate.queryForList(
                    "SELECT a.id userID,b.role_id roleID,a.branch_no,c.name roleName "
                            + "FROM users a "
                            + "INNER JOIN user_roles b on b.user_id=a.id "
                            + "INNER JOIN roles c on c.id=b.role_id "
                            + "WHERE lower(a.username)=lower(?) and a.status='ACTIVE'",
                    username);
        } catch (Exception e) {
            LOGGER.info("EXCEPTION: {}", e.getMessage());
        }
        return result;
    }

    public List<Map<String, Object>> getModulesPermissions() {
        List<Map<String, Object>> result = null;
        try {
            result = this.jdbcTemplate.queryForList("SELECT c.id moduleID,c.name moduleName,a.id permID,a.name permissionName FROM permissions a INNER JOIN module_permission b on b.permission_id=a.id INNER join modules c on c.id=b.module_id");
        } catch (Exception e) {
            LOGGER.info("EXCEPTION: {}", e.getMessage());
        }
        return result;
    }

    public List<Map<String, Object>> getOperationsPermission(String roleId) {
        try {
            return this.jdbcTemplate.queryForList("select a.name,a.url,a.ajax_url,a.description from recon_operations a INNER JOIN recon_operation_role b on b.recon_operation_id=a.id WHERE b.role_id=?", roleId);
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            return null;
        }
    }

    public List<Map<String, Object>> getSubModulesPermissions() {
        try {
            return this.jdbcTemplate.queryForList("SELECT  url,ajax_url FROM report_setup");
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            return null;
        }
    }

    public List<Map<String, Object>> getRoles() {
        try {
            return this.jdbcTemplate.queryForList("SELECT * from roles where id!=1");
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            return null;
        }
    }

    public List<Map<String, Object>> branches() {
        try {
            return this.jdbcTemplate.queryForList("SELECT * from branches");
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            return null;
        }
    }

    /*
    get all recon Type by role_id
     */
    public List<Map<String, Object>> getReconTypePerRole(String role_id) {
        try {
            return this.jdbcTemplate.queryForList("SELECT * from recon_types_roles where role_id=?", role_id);
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            return null;
        }
    }

    /*
    get all recon operation role by role_id
     */
    public List<Map<String, Object>> getReconOperationPerRole(String role_id) {
        try {
            return this.jdbcTemplate.queryForList("SELECT * from recon_operation_role where  role_id=?", role_id);
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            return null;
        }
    }

    /*
    get all TRANSFER TYPES PER  role by GIVEN role_id
     */
    public List<Map<String, Object>> getTransferTypesPerRole(String role_id) {
        try {
            return this.jdbcTemplate.queryForList("SELECT * from transfer_type_role where  role_id=?", role_id);
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            return null;
        }
    }

    /*
    get all TRANSFER TYPES PER  role by GIVEN role_id
     */
    public List<Map<String, Object>> getPaymentsModulePerRole(String role_id) {
        try {
            return this.jdbcTemplate.queryForList("SELECT * from transfer_type_role where  role_id=?", role_id);
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            return null;
        }
    }

    public List<Map<String, Object>> getRole(String role_id) {
        try {
            return this.jdbcTemplate.queryForList("SELECT * from roles where id!=1 and id=?", role_id);
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            return null;
        }
    }

    public List<Map<String, Object>> getModules() {
        try {
            return this.jdbcTemplate.queryForList("SELECT name,id FROM `modules`");
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            return null;
        }
    }

    public List<Map<String, Object>> getPaymentsModules() {
        try {
            return this.jdbcTemplate.queryForList("SELECT * FROM payment_modules");
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            return null;
        }
    }

    public List<Map<String, Object>> getPaymentsPermissions() {
        try {
            return this.jdbcTemplate.queryForList("SELECT c.id moduleID,c.name moduleName,a.id permID,a.name permissionName FROM payment_permissions a INNER JOIN payment_module_permission b on b.permission_id=a.id INNER join payment_modules c on c.id=b.module_id");
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            return null;
        }
    }

    public List<Map<String, Object>> getAllUsers() {
        try {
            return this.jdbcTemplate.queryForList("SELECT a.id,a.first_name,a.middle_name,a.last_name,a.username,a.email,c.name as roleName,a.phoneNo,a.status FROM users a INNER join user_roles b on a.id=b.user_id INNER JOIN roles c on c.id=b.role_id");
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            return null;
        }
    }

    public UserForm getUserDetails(String userId) {
        UserForm user = new UserForm();
        try {
            List<Map<String, Object>> us = this.jdbcTemplate.queryForList("SELECT a.id,a.first_name,a.middle_name,a.last_name,a.username,a.email,c.name as roleName,a.phoneNo,'Active' status,c.id roleId,(select code from branches where code=a.branch_no limit 1) branchCode FROM users a LEFT join user_roles b on a.id=b.user_id LEFT JOIN roles c on c.id=b.role_id where a.id=?", userId);
            for (Map<String, Object> detail : us) {
                user.setBranchCode(detail.get("branchCode") + "");
                user.setEmail(detail.get("email") + "");
                user.setFirstName(detail.get("first_name") + "");
                user.setLastName(detail.get("last_name") + "");
                user.setMiddleName(detail.get("middle_name") + "");
                user.setPhone(detail.get("phoneNo") + "");
                user.setRole(detail.get("roleId") + "");
                user.setUsername(detail.get("username") + "");
                user.setTrackingId(detail.get("id") + "");
            }
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            return user;
        }
        return user;
    }

    /*
 * get the recon types 
     */
    public List<Map<String, Object>> getReconTypes() {
        try {
            return this.jdbcTemplate.queryForList("SELECT * FROM recon_types");
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            return null;
        }
    }

    public List<Map<String, Object>> getGeneralReconTypes() {
        try {
            return this.jdbcTemplate.queryForList("SELECT * FROM general_reconciliation_configs");
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            return null;
        }
    }

    public List<Map<String, Object>> getGeneralReconTypes(String roleId) {
        try {
            return this.jdbcTemplate.queryForList("SELECT * FROM general_reconciliation_configs grc inner join " +
                    "general_reconciliation_config_roles rgrc on grc.id = rgrc.grc_id where role_id = ?", roleId);
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            return null;
        }
    }

    /**
     *
     * @get the roles mapped per module
     */
    public List<Map<String, Object>> getPermissionPerModule(String role_id) {
        try {
            return this.jdbcTemplate.queryForList("SELECT * FROM permission_role where role_id=?", role_id);
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            return null;
        }
    }

    /**
     *
     * @get the roles mapped per payment permission
     */
    public List<Map<String, Object>> getPaymentPermissionPerModule(String role_id) {
        try {
            return this.jdbcTemplate.queryForList("SELECT * FROM payment_permission_role where role_id=?", role_id);
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            return null;
        }
    }

    public List<Map<String, Object>> getReconOperationsTypes() {
        try {
            return this.jdbcTemplate.queryForList("SELECT * FROM recon_operations");
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            return null;
        }
    }

    /*
    get transfer types
     */
    public List<Map<String, Object>> getTransferTypes() {
        try {
            return this.jdbcTemplate.queryForList("SELECT * FROM transfer_type");
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            return null;
        }
    }

//    public String getUsersList(String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
//        /**
//         * AppO - OppA *
//         */
//        System.out.println("draw:" + draw);
//        System.out.println("start:" + start);
//        System.out.println("rowPerPage:" + rowPerPage);
//        System.out.println("searchValue:" + searchValue);
//        System.out.println("columnIndex:" + columnIndex);
//        System.out.println("columnName:" + columnName);
//        System.out.println("columnSortOrder:" + columnSortOrder);
//        /**
//         * Pamoja - Pamoja *
//         */
//
//        int totalRecordwithFilter = 0;
//        int totalRecords = jdbcTemplate.queryForObject("SELECT count(*) FROM users", Integer.class);
//        String searchQuery = "";
//        if (!searchValue.equals("")) {
//            searchValue = "%" + searchValue + "%";
//            searchQuery = "WHERE name LIKE ? OR username LIKE ?";
//            totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT count(*) FROM users " + searchQuery, new Object[]{searchValue, searchValue}, Integer.class);
//
//        } else {
//            totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT count(*) FROM users ", Integer.class);
//        }
//
//        List<Map<String, Object>> findAll;
//        String mainSql = "select a.*,'actions','roles' from users a ORDER BY " + columnName + " " + columnSortOrder + " LIMIT " + start + "," + rowPerPage;
//        if (!searchQuery.equals("")) {
//            mainSql = "select curr_id,"
//                    + "  name,\n"
//                    + "  code, CASE WHEN rec_st = '1' THEN 'ACTIVE' WHEN rec_st = 0 THEN 'INACTIVE' END as rec_st "
//                    + " from st_currency " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " LIMIT " + start + "," + rowPerPage;
//            findAll = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, searchValue});
//        } else {
//            findAll = this.jdbcTemplate.queryForList(mainSql);
//        }
//
//        //Java objects to JSON string - compact-print - salamu - Pomoja.
//        String jsonString = null;
//        try {
//            System.out.println("FIND ALL QUERY CHECK:" + findAll);
//            jsonString = this.jacksonMapper.writeValueAsString(findAll);
//            //LOGGER.info("RequestBody");
//        } catch (JsonProcessingException ex) {
//            LOGGER.info("RequestBody: ", ex);
//        }
//        //System.out.println("findAll:" + findAll.toString());
//        String json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
//
//        System.out.println("findAll:" + json);
//
//        return json;
//    }

    public String getUsersList(String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        int totalRecordwithFilter = 0;
        int totalRecords = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        String searchQuery = "";
        if (!searchValue.equals("")) {
            searchValue = "%" + searchValue + "%";
            searchQuery = "WHERE  concat(first_name,' ',middle_name,' ',last_name,' ',username,' ',email,' ',phoneNo,' ',password,' ',status,' ',branch_no) LIKE ?";
            totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT count(*) FROM users " + searchQuery, new Object[]{searchValue}, Integer.class);
        } else {
            totalRecordwithFilter = totalRecords;
        }

        List<Map<String, Object>> findAll;
        String mainSql = "select a.*,'actions','roles' FROM users a";
        if (!searchQuery.equals("")) {
            mainSql = "select a.*,'actions','roles' FROM users a " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY ";
            findAll = jdbcTemplate.queryForList(mainSql, new Object[]{searchValue});
        } else {
            findAll = jdbcTemplate.queryForList(mainSql);
            LOGGER.info("Without search  value users : {}", findAll);

        }

        String jsonString = null;
        try {
            jsonString = this.jacksonMapper.writeValueAsString(findAll);
        } catch (JsonProcessingException ex) {
            LOGGER.error("USERS REQ BODY: ", ex);
        }
        String json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
        return json;
    }

    //@Transactional
    public int saveRole(String roleName, String Description, String permissionId, String reconTypeId, String generalReconTypeID, String operationId, String moduleId, String created_by, String tansferId, String paymentPermissionId, String kycId) {
        //save the role and get the last insert ID as role ID
        int roleID;
        final String INSERT_SQL = "insert into roles (name,description,created_by,status) values(?,?,?,?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(new PreparedStatementCreator() {
            public PreparedStatement createPreparedStatement(@Nonnull Connection connection) throws SQLException {
                PreparedStatement ps = connection.prepareStatement(INSERT_SQL, new String[]{"id"});
                ps.setString(1, roleName);
                ps.setString(2, Description);
                ps.setString(3, created_by);
                ps.setString(4, "Active");
                return ps;
            }
        }, keyHolder);
        roleID = keyHolder.getKey().intValue();
        //Add MODULE permission to the created role
        if (permissionId.contains("&")) {
            String[] params = permissionId.split("&");
            for (String param : params) {
                if (param.contains("=")) {
                    String name = param.split("=")[0];
                    String permID = param.split("=")[1].split("__")[0];
                    String moduleID = param.split("=")[1].split("__")[1];
                    Integer result = jdbcTemplate.update("insert into  permission_role(permission_id,role_id,created_by) values(?,?,?)", permID, roleID, created_by);
                    //add role to module per specific permission
                    Integer moduleRole = jdbcTemplate.update("insert ignore into  module_roles(module_id,role_id,created_by) values(?,?,?)", moduleID, roleID, created_by);
                    //allow module permission as well
//                    Integer module_permission = jdbcTemplate.update("insert ignore into  module_permission(module_id,permission_id,created_by) values(?,?,?)", moduleID, permID, created_by);
                }
            }
        }
        if (permissionId.contains("=") && !permissionId.contains("&")) {
            String name = permissionId.split("=")[0];
            String permID = permissionId.split("=")[1].split("__")[0];
            String moduleID = permissionId.split("=")[1].split("__")[1];
            Integer result = jdbcTemplate.update("insert into  permission_role(permission_id,role_id,created_by) values(?,?,?)", permID, roleID, created_by);
            //add role to module per specific permission
            Integer moduleRole = jdbcTemplate.update("insert ignore into  module_roles(module_id,role_id,created_by) values(?,?,?)", moduleID, roleID, created_by);
            //allow module permission as well
//            Integer module_permission = jdbcTemplate.update("insert ignore into  module_permission(module_id,permission_id,created_by) values(?,?,?)", moduleID, permID, created_by);
        }
        //Add RECON TYPES permission to the created role
        if (reconTypeId.contains("&")) {
            String[] params = reconTypeId.split("&");
            for (String param : params) {
                if (param.contains("=")) {
                    String name = param.split("=")[0];
                    String recon_type_id = param.split("=")[1];
                    Integer result = jdbcTemplate.update("insert into   recon_types_roles(recon_type_id,role_id,created_by) values(?,?,?)", recon_type_id, roleID, created_by);
                }
            }
        }
        if (reconTypeId.contains("=") && !reconTypeId.contains("&")) {
            String name = reconTypeId.split("=")[0];
            String recon_type_id = reconTypeId.split("=")[1];
            Integer result = jdbcTemplate.update("insert into   recon_types_roles(recon_type_id,role_id,created_by) values(?,?,?)", recon_type_id, roleID, created_by);

        }
        //Add GENERAL RECON TYPES permission to the created role
        if (generalReconTypeID.contains("&")) {
            String[] params = generalReconTypeID.split("&");
            for (String param : params) {
                if (param.contains("=")) {
                    String name = param.split("=")[0];
                    String grc_id = param.split("=")[1];
                    Integer result = jdbcTemplate.update("insert into general_reconciliation_config_roles(grc_id,role_id,created_by) values(?,?,?)", grc_id, roleID, created_by);
                }
            }
        }
        if (generalReconTypeID.contains("=") && !generalReconTypeID.contains("&")) {
            String name = generalReconTypeID.split("=")[0];
            String grc_id = generalReconTypeID.split("=")[1];
            Integer result = jdbcTemplate.update("insert into general_reconciliation_config_roles(grc_id,role_id,created_by) values(?,?,?)", grc_id, roleID, created_by);

        }
        //Add RECON OPERATIONS permission to the created role
        if (operationId.contains("&")) {
            String[] params = operationId.split("&");
            for (String param : params) {
                if (param.contains("=")) {
                    String name = param.split("=")[0];
                    String recon_operation_id = param.split("=")[1];
                    Integer result = jdbcTemplate.update("insert into   recon_operation_role(recon_operation_id,role_id,created_by) values(?,?,?)", recon_operation_id, roleID, created_by);
                }
            }
        }
        if (operationId.contains("=") && !operationId.contains("&")) {
            String name = operationId.split("=")[0];
            String recon_operation_id = operationId.split("=")[1];
            Integer result = jdbcTemplate.update("insert into   recon_operation_role(recon_operation_id,role_id,created_by) values(?,?,?)", recon_operation_id, roleID, created_by);

        }
        //Add TRANSFER TYPE permission to the created role
        if (tansferId.contains("&")) {
            String[] params = tansferId.split("&");
            for (String param : params) {
                if (param.contains("=")) {
                    String name = param.split("=")[0];
                    String transfer_type_id = param.split("=")[1];
                    LOGGER.info("insert into   transfer_type_role(transfer_type_id,role_id,created_by) values('{}','{}','{}')", transfer_type_id, roleID, created_by);

                    Integer result = jdbcTemplate.update("insert into   transfer_type_role(transfer_type_id,role_id,created_by) values(?,?,?)", transfer_type_id, roleID, created_by);
                }
            }
        }
        if (tansferId.contains("=") && !tansferId.contains("&")) {
            String name = tansferId.split("=")[0];
            String transfer_type_id = tansferId.split("=")[1];
            LOGGER.info("insert into   transfer_type_role(transfer_type_id,role_id,created_by) values('{}','{}','{}')", transfer_type_id, roleID, created_by);

            Integer result = jdbcTemplate.update("insert into   transfer_type_role(transfer_type_id,role_id,created_by) values(?,?,?)", transfer_type_id, roleID, created_by);

        }
        if (paymentPermissionId != null) {
            //Add PAYMENT MODULE permission to the created role
            if (paymentPermissionId.contains("&")) {
                String[] params = paymentPermissionId.split("&");
                for (String param : params) {
                    if (param.contains("=")) {
                        String name = param.split("=")[0];
                        String permID = param.split("=")[1].split("__")[0];
                        String moduleID = param.split("=")[1].split("__")[1];
                        Integer result = jdbcTemplate.update("insert into  payment_permission_role(payment_permission_id,role_id,created_by) values(?,?,?)", permID, roleID, created_by);
                        //add role to module per specific permission
                        Integer moduleRole = jdbcTemplate.update("insert ignore into  payment_module_role(module_id,role_id,created_by) values(?,?,?)", moduleID, roleID, created_by);
                        //allow module permission as well
//                    Integer module_permission = jdbcTemplate.update("insert ignore into  module_permission(module_id,permission_id,created_by) values(?,?,?)", moduleID, permID, created_by);
                    }
                }
            }
            if (paymentPermissionId.contains("=") && !paymentPermissionId.contains("&")) {
                String name = paymentPermissionId.split("=")[0];
                String permID = paymentPermissionId.split("=")[1].split("__")[0];
                String moduleID = paymentPermissionId.split("=")[1].split("__")[1];
                Integer result = jdbcTemplate.update("insert into  payment_permission_role(payment_permission_id,role_id,created_by) values(?,?,?)", permID, roleID, created_by);
                //add role to module per specific permission
                Integer moduleRole = jdbcTemplate.update("insert ignore into  payment_module_role(module_id,role_id,created_by) values(?,?,?)", moduleID, roleID, created_by);
                //allow module permission as well
//            Integer module_permission = jdbcTemplate.update("insert ignore into  module_permission(module_id,permission_id,created_by) values(?,?,?)", moduleID, permID, created_by);
            }
        }
        if (kycId != null) {
            if (kycId.contains("&")) {
                String[] arrayOfString = kycId.split("&");
                for (String str : arrayOfString) {
                    if (str.contains("=")) {
                        String str1 = str.split("=")[0];
                        String str2 = str.split("=")[1];
                        Integer integer = Integer.valueOf(this.jdbcTemplate.update("insert into kyc_permission_role(kyc_permission_id,role_id,created_by) values(?,?,?)", new Object[]{str2, Integer.valueOf(roleID), created_by}));
                    }
                }
            }
            if (kycId.contains("=") && !kycId.contains("&")) {
                String str1 = kycId.split("=")[0];
                String str2 = kycId.split("=")[1];
                LOGGER.info("insert into kyc_permission_role(kyc_permission_id,role_id,created_by) values('{}','{}','{}')", new Object[]{str2, Integer.valueOf(roleID), created_by});
                Integer integer = Integer.valueOf(this.jdbcTemplate.update("insert into kyc_permission_role(kyc_permission_id,role_id,created_by) values(?,?,?)", new Object[]{str2, Integer.valueOf(roleID), created_by}));
            }
        }
        return roleID;
    }

    //@Transactional
    public String editRole(String roleName, String Description, String permissionId, String reconTypeId, String generalReconTypeID, String operationId, String moduleId, String role_id, String created_by, String transferID, String paymentPermissionId, String kycId) {
        /*delete all permission belong to this role_id*/
        deleteRolePermission(role_id);
//save the role and get the last insert ID as role ID
        String roleID;
        final String INSERT_SQL = "update roles set name=?,description=? where id=?";
        Integer updateRole = jdbcTemplate.update(INSERT_SQL, roleName, Description, role_id);

        //Add MODULE permission to the created role
        if (permissionId.contains("&")) {
            String[] params = permissionId.split("&");
            for (String param : params) {
                if (param.contains("=")) {
                    String name = param.split("=")[0];
                    String permID = param.split("=")[1].split("__")[0];
                    String moduleID = param.split("=")[1].split("__")[1];
                    Integer result = jdbcTemplate.update("insert into  permission_role(permission_id,role_id,created_by) values(?,?,?)", permID, role_id, created_by);
                    //add role to module per specific permission
                    Integer moduleRole = jdbcTemplate.update("insert ignore into  module_roles(module_id,role_id,created_by) values(?,?,?)", moduleID, role_id, created_by);
                    //allow module permission as well
//                    Integer module_permission = jdbcTemplate.update("insert ignore into  module_permission(module_id,permission_id,created_by) values(?,?,?)", moduleID, permID, created_by);
                }
            }
        }
        if (permissionId.contains("=") && !permissionId.contains("&")) {
            String name = permissionId.split("=")[0];
            String permID = permissionId.split("=")[1].split("__")[0];
            String moduleID = permissionId.split("=")[1].split("__")[1];
            Integer result = jdbcTemplate.update("insert into  permission_role(permission_id,role_id,created_by) values(?,?,?)", permID, role_id, created_by);
            //add role to module per specific permission
            Integer moduleRole = jdbcTemplate.update("insert ignore into  module_roles(module_id,role_id,created_by) values(?,?,?)", moduleID, role_id, created_by);
            //allow module permission as well
//            Integer module_permission = jdbcTemplate.update("insert ignore into  module_permission(module_id,permission_id,created_by) values(?,?,?)", moduleID, permID, created_by);
        }
        //Add RECON TYPES permission to the created role
        if (reconTypeId.contains("&")) {
            String[] params = reconTypeId.split("&");
            for (String param : params) {
                if (param.contains("=")) {
                    String name = param.split("=")[0];
                    String recon_type_id = param.split("=")[1];
                    Integer result = jdbcTemplate.update("insert into   recon_types_roles(recon_type_id,role_id,created_by) values(?,?,?)", recon_type_id, role_id, created_by);
                }
            }
        }
        if (reconTypeId.contains("=") && !reconTypeId.contains("&")) {
            String name = reconTypeId.split("=")[0];
            String recon_type_id = reconTypeId.split("=")[1];
            Integer result = jdbcTemplate.update("insert into   recon_types_roles(recon_type_id,role_id,created_by) values(?,?,?)", recon_type_id, role_id, created_by);

        }
        //Add GENERAL RECON TYPES permission to the created role
        if (generalReconTypeID.contains("&")) {
            String[] params = generalReconTypeID.split("&");
            for (String param : params) {
                if (param.contains("=")) {
                    String name = param.split("=")[0];
                    String grc_id = param.split("=")[1];
                    Integer result = jdbcTemplate.update("insert into general_reconciliation_config_roles(grc_id,role_id,created_by) values(?,?,?)", grc_id, role_id, created_by);
                }
            }
        }
        if (generalReconTypeID.contains("=") && !generalReconTypeID.contains("&")) {
            String name = generalReconTypeID.split("=")[0];
            String grc_id = generalReconTypeID.split("=")[1];
            Integer result = jdbcTemplate.update("insert into general_reconciliation_config_roles(grc_id,role_id,created_by) values(?,?,?)", grc_id, role_id, created_by);
        }
        //Add RECON OPERATIONS permission to the created role
        if (operationId.contains("&")) {
            String[] params = operationId.split("&");
            for (String param : params) {
                if (param.contains("=")) {
                    String name = param.split("=")[0];
                    String recon_operation_id = param.split("=")[1];
                    Integer result = jdbcTemplate.update("insert into   recon_operation_role(recon_operation_id,role_id,created_by) values(?,?,?)", recon_operation_id, role_id, created_by);
                }
            }
        }
        if (operationId.contains("=") && !operationId.contains("&")) {
            String name = operationId.split("=")[0];
            String recon_operation_id = operationId.split("=")[1];
            Integer result = jdbcTemplate.update("insert into   recon_operation_role(recon_operation_id,role_id,created_by) values(?,?,?)", recon_operation_id, role_id, created_by);

        }
        //Add TRANSFER TYPE permission to the created role
        if (transferID.contains("&")) {
            String[] params = transferID.split("&");
            for (String param : params) {
                if (param.contains("=")) {
                    String name = param.split("=")[0];
                    String transfer_type_id = param.split("=")[1];
                    Integer result = jdbcTemplate.update("insert into   transfer_type_role(transfer_type_id,role_id,created_by) values(?,?,?)", transfer_type_id, role_id, created_by);
                }
            }
        }
        if (transferID.contains("=") && !transferID.contains("&")) {
            String name = transferID.split("=")[0];
            String transfer_type_id = transferID.split("=")[1];
            LOGGER.info("insert into   transfer_type_role(transfer_type_id,role_id,created_by) values('{}','{}','{}')", transfer_type_id, role_id, created_by);
            Integer result = jdbcTemplate.update("insert into   transfer_type_role(transfer_type_id,role_id,created_by) values(?,?,?)", transfer_type_id, role_id, created_by);
        }
        //Add PAYMENT MODULE permission to the edited role
        if (paymentPermissionId.contains("&")) {
            String[] params = paymentPermissionId.split("&");
            for (String param : params) {
                if (param.contains("=")) {
                    String name = param.split("=")[0];
                    String permID = param.split("=")[1].split("__")[0];
                    String moduleID = param.split("=")[1].split("__")[1];
                    Integer result = jdbcTemplate.update("insert into  payment_permission_role(payment_permission_id,role_id,created_by) values(?,?,?)", permID, role_id, created_by);
                    //add role to module per specific permission
//                    Integer moduleRole = jdbcTemplate.update("insert ignore into  payment_module_role(module_id,role_id,created_by) values(?,?,?)", moduleID, role_id, created_by);
                    //allow module permission as well
//                    Integer module_permission = jdbcTemplate.update("insert ignore into  module_permission(module_id,permission_id,created_by) values(?,?,?)", moduleID, permID, created_by);
                }
            }
        }
        if (paymentPermissionId.contains("=") && !paymentPermissionId.contains("&")) {
            String name = paymentPermissionId.split("=")[0];
            String permID = paymentPermissionId.split("=")[1].split("__")[0];
            String moduleID = paymentPermissionId.split("=")[1].split("__")[1];
            Integer result = jdbcTemplate.update("insert into  payment_permission_role(payment_permission_id,role_id,created_by) values(?,?,?)", permID, role_id, created_by);
            //add role to module per specific permission
//            Integer moduleRole = jdbcTemplate.update("insert ignore into  payment_module_role(module_id,role_id,created_by) values(?,?,?)", moduleID, role_id, created_by);
            //allow module permission as well
//            Integer module_permission = jdbcTemplate.update("insert ignore into  module_permission(module_id,permission_id,created_by) values(?,?,?)", moduleID, permID, created_by);
        }
        if (kycId.contains("&")) {
            String[] arrayOfString = kycId.split("&");
            for (String str : arrayOfString) {
                if (str.contains("=")) {
                    String str1 = str.split("=")[0];
                    String str2 = str.split("=")[1];
                    Integer integer1 = Integer.valueOf(this.jdbcTemplate.update("insert into kyc_permission_role(kyc_permission_id,role_id,created_by) values(?,?,?)", new Object[]{str2, role_id, created_by}));
                }
            }
        }
        if (kycId.contains("=") && !kycId.contains("&")) {
            String str1 = kycId.split("=")[0];
            String str2 = kycId.split("=")[1];
            LOGGER.info("insert into kyc_permission_role(kyc_permission_id,role_id,created_by) values('{}','{}','{}')", new Object[]{str2, role_id, created_by});
            Integer integer1 = Integer.valueOf(this.jdbcTemplate.update("insert into kyc_permission_role(kyc_permission_id,role_id,created_by) values(?,?,?)", new Object[]{str2, role_id, created_by}));
        }
        return role_id;
    }

    /*
    remove all roles on editing
     */
    @Transactional
    public int deleteRolePermission(String role_id) {
        try {
            Integer result = jdbcTemplate.update("delete  FROM recon_types_roles where role_id=?", role_id);
            Integer result3 = jdbcTemplate.update("delete  FROM permission_role where role_id=?", role_id);
            Integer result4 = jdbcTemplate.update("delete  FROM module_roles where role_id=?", role_id);
            Integer result2 = jdbcTemplate.update("delete  FROM recon_operation_role where role_id=?", role_id);
            Integer result5 = jdbcTemplate.update("delete  FROM transfer_type_role where role_id=?", role_id);
            Integer result6 = jdbcTemplate.update("delete  FROM payment_permission_role where role_id=?", role_id);
            Integer result7 = jdbcTemplate.update("delete  FROM kyc_permission_role where role_id=?", role_id);
            Integer result8 = jdbcTemplate.update("delete  FROM general_reconciliation_config_roles where role_id=?", role_id);
//        String sql = "delete a.*,b.*,c.*,d.* FROM recon_types_roles  a JOIN permission_role b on b.role_id=a.role_id JOIN module_roles c on c.role_id=a.role_id join recon_operation_role d on d.role_id=a.role_id where a.role_id=?";
//        Integer result = jdbcTemplate.update(sql, role_id);
            return result;
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            return -1;
        }
    }

    //@Transactional
    public int saveUser(String firstName, String middleName, String lastName, String username, String email, String phone, String role, String created_by, String branch_no, String status) {
        //save the role and get the last insert ID as role ID
        int userID;
        int result = -1;
        try {
            String password = "password";
            final String INSERT_SQL = "INSERT INTO users(first_name, middle_name, last_name, username, email, phoneNo,password,branch_no,status) values(?,?,?,?,?,?,?,?,?)";
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update((Connection connection) -> {
                PreparedStatement ps = connection.prepareStatement(INSERT_SQL, new String[]{"id"});
                ps.setString(1, firstName);
                ps.setString(2, middleName);
                ps.setString(3, lastName);
                ps.setString(4, username);
                ps.setString(5, email);
                ps.setString(6, phone);
                ps.setString(7, new BCryptPasswordEncoder().encode(password));
                ps.setString(8, branch_no);
                ps.setString(9, status);
                return ps;
            }, keyHolder);
            userID = Objects.requireNonNull(keyHolder.getKey()).intValue();

            //mapp a user to the role after inserting the user
            result = jdbcTemplate.update("insert into user_roles(user_id,role_id,created_by) values(?,?,?)", userID, role, created_by);
            LOGGER.info("USER CREATED WITH ID: {} AND ROLE SAVED: {}", userID, result);
        } catch (DataAccessException e) {
            LOGGER.info("User_M:saveUser: {}", e);
        }
        return result;
    }

    public int editUser(UserForm userForm, String modifiedBy, String custId) {
        int result = -1;
        //save the role and get the last insert ID as role ID
        try {
            result = jdbcTemplate.update("UPDATE users SET first_name=?,middle_name=?,last_name=?,username=?,email=?,phoneNo=?,branch_no=?,modified_dt=?,modified_by=?,status=? WHERE id=?",
                    userForm.getFirstName(), userForm.getMiddleName(), userForm.getLastName(), userForm.getUsername(), userForm.getEmail(), userForm.getPhone(), userForm.getBranchCode(), DateUtil.now(), modifiedBy, userForm.getUserStatus(), custId);
            result = jdbcTemplate.update("UPDATE user_roles SET role_id=?,modified_by=?, modified_dt=? WHERE user_id=?", userForm.getRole(), modifiedBy, DateUtil.now(), custId);
            LOGGER.info("USER UPDATED SUCCESSFULLY WITH ID: {} ", custId);
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.info("EXCEPTION OCCURRED DURING UPDATING USER: {}", userForm.getUsername());
        }
        return result;
    }

    public String getUserListsAjax(String branchCode, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        switch (branchCode) {
            case "0":
                try {
                    mainSql = "select count(*)  from users";
                    totalRecords = jdbcTemplate.queryForObject(mainSql, Integer.class);
                    String searchQuery = "";
                    if (!searchValue.equals("")) {
                        searchValue = "%" + searchValue + "%";
                        searchQuery = " WHERE  concat(first_name ,' ',middle_name,' ',last_name,' ',username,' ',email,' ',phoneNo,' ',last_login) LIKE ?";
                        totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users a left join user_roles b on a.id=b.user_id " + searchQuery, new Object[]{searchValue}, Integer.class);
                    } else {
                        totalRecordwithFilter = totalRecords;
                    }
                    if (!searchQuery.equals("")) {
                        mainSql = "SELECT a.*,(SELECT name from branches where  code=a.branch_no) branchName,(select c.name from roles c where c.id=b.role_id) roleName  FROM users a left join user_roles b on a.id=b.user_id " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                        results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue});
                    } else {
                        mainSql = "SELECT a.*,(SELECT name from branches where  code=a.branch_no limit 1) branchName,(select c.name from roles c where c.id=b.role_id) roleName  FROM users a left join user_roles b on a.id=b.user_id ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                        results = this.jdbcTemplate.queryForList(mainSql);
                    }
                    jsonString = this.jacksonMapper.writeValueAsString(results);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    LOGGER.error("RequestBody: ", ex);
                }
              break;

            default:
                try {
                    mainSql = "select count(*) from users  where branch_no=?";
                    totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{branchCode}, Integer.class);
                    String searchQuery = "";
                    if (!searchValue.equals("")) {
                        searchValue = "%" + searchValue + "%";
                        searchQuery = " WHERE  concat(first_name,' ', middle_name,' ',last_name,' ',username,' ',email,' ',phoneNo,' ',last_login) LIKE ? and branch_no=?";
                        totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(*)  FROM users a left join user_roles b on a.id=b.user_id" + searchQuery, new Object[]{searchValue, branchCode}, Integer.class);
                    } else {
                        totalRecordwithFilter = totalRecords;
                    }
                    if (!searchQuery.equals("")) {
                        mainSql = "SELECT a.*,(SELECT name from branches where code=a.branch_no)  branchName,(select c.name from roles c where c.id=b.role_id) roleName  FROM users a left join user_roles b on a.id=b.user_id " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                        results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, branchCode});

                    } else {
                        mainSql = "SELECT a.*,(SELECT name from branches where code=a.branch_no limit 1)  branchName,(select c.name from roles c where c.id=b.role_id) roleName  FROM users a left join user_roles b on a.id=b.user_id  WHERE a.branch_no=? ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                        results = this.jdbcTemplate.queryForList(mainSql, new Object[]{branchCode});
                    }
                    jsonString = this.jacksonMapper.writeValueAsString(results);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    LOGGER.error("RequestBody: ", ex);
                }
               break;
        }
        return "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
    }

    /*
    UPDATE LAST LOGIN IN USERS TABLE
     */
    public int updateLastLoginEvent(String userId) {
        int lastUpdate = -1;
        try {
            lastUpdate = jdbcTemplate.update("UPDATE USERS set last_login=? where id=?", DateUtil.now(), userId);
            return lastUpdate;
        } catch (Exception e) {
            return lastUpdate;
        }
    }

    public String getUserMatrixReport(String exporterFileType, HttpServletResponse response, String destName, String printedBy) {
        String reportFileTemplate = "/iReports/cilantro-user-matrix.jasper";
        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("PRINT_DATE", DateUtil.now("yyyy-MM-dd HH:mm:ss"));
            parameters.put("PRINTED_BY", printedBy.toUpperCase());
            JasperPrint print = jasperService.jasperPrint(reportFileTemplate, parameters, cilantroDataSource.getConnection());
            return jasperService.exportFileOption(print, exporterFileType, response, destName);
        } catch (Exception ex) {
            Logger.getLogger(ReconReportsRepo.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    //log Audit trail
    public int logAudit(AuditLogEvent req) {
        int result;
        try {
            String sql = "INSERT INTO audit_logs(username, role_id, branchNo, ip_address, function_name, status, comments)" +
                    " VALUES(?,?,?,?,?,?,?)";

            result = jdbcTemplate.update(sql,
                    req.getUsername(), req.getRoleId(), req.getBranchNo(), req.getIpAddress(), req.getFunctionName(), req.getStatus(), req.getComments());
        } catch (DataAccessException e) {
            result = -1;
            // LOGGER.info("Roll backed... {}", e.getMessage());
            return result;
        }
        return result;
    }

    public List<Map<String, Object>> getKycModules() {
        try {
            return this.jdbcTemplate.queryForList("SELECT * FROM kyc_modules");
        } catch (Exception exception) {
            LOGGER.info(null, exception);
            return null;
        }
    }

    public List<Map<String, Object>> getKycModulesPerRole(String paramString) {
        try {
            return this.jdbcTemplate.queryForList("SELECT * from kyc_permission_role where role_id=?", new Object[]{paramString});
        } catch (Exception exception) {
            LOGGER.info(null, exception);
            return null;
        }
    }

    public List<Map<String, Object>> getKycModulesPermissionsPerRole(String paramString) {
        try {
            return this.jdbcTemplate.queryForList("SELECT a.name,a.url,a.ajax_url,a.sub_url FROM kyc_modules a INNER JOIN kyc_permission_role b on a.id=b.kyc_permission_id where role_id=?", new Object[]{paramString});
        } catch (Exception exception) {
            LOGGER.info(null, exception);
            return null;
        }
    }


    public List<Map<String, Object>> getAuthorizedModules(String roleId, String moduleUrl) {
        List<Map<String,Object>> finalRes=null;
        String sql;
        try{
            sql="SELECT w.id as work_flow_id,w.name as work_flow_name, w.code,pm.name as module_name,r.name as user_role FROM workflow w INNER JOIN payment_module_workflow pmw ON w.id=pmw.workflow_id INNER JOIN payment_modules pm on pm.id = pmw.module_id INNER JOIN roles r on r.id = pmw.role_id AND  pm.module_dashboard_url=? AND r.id=?";
            finalRes= this.jdbcTemplate.queryForList(sql, moduleUrl,roleId);
        }catch (DataAccessException dae){
            LOGGER.info("Data access exception in fetching user backflow... {}", dae);
        }
        return finalRes;
    }
}
