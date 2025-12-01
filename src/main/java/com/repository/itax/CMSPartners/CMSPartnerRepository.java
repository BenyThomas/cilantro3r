package com.repository.itax.CMSPartners;

import com.DTO.cms.CMSPartnerForm;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.models.PartnerReferences;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class CMSPartnerRepository {

    @Autowired
    @Qualifier("partners")
    JdbcTemplate jdbcTemplate;

    @Autowired
    ObjectMapper jacksonMapper;

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CMSPartnerRepository.class);

    public String queryCMSPartnersAjax(String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        int totalRecordwithFilter = 0;
        int totalRecords = jdbcTemplate.queryForObject("SELECT COUNT(a.acct_no) FROM ega_partners a WHERE a.partner_status='A'", Integer.class);
        String searchQuery = "";
        if (!searchValue.equals("")) {
            searchValue = "%" + searchValue + "%";
            searchQuery = "WHERE partner_status='A' AND concat(partner_name,' ',acct_no,' ',purpose,' ',partner_type,' ',partner_code, ' ',bot_acct) LIKE ?";
            totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT count(a.acct_no) FROM ega_partners a " + searchQuery, new Object[]{searchValue}, Integer.class);
        } else {
            totalRecordwithFilter = totalRecords;
        }

        List<Map<String, Object>> findAll;
        String mainSql = "select * FROM ega_partners WHERE partner_status = 'A' ";
        if (!searchQuery.equals("")) {
            mainSql = "select *  from ega_partners " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY ";
            findAll = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue});
            LOGGER.info("results CMS PARTNERS : {}", findAll);
        } else {
            findAll = this.jdbcTemplate.queryForList(mainSql);
            LOGGER.info("Without search  value CMS PARTNERS : {}", findAll);

        }

        String jsonString = null;
        try {
            jsonString = this.jacksonMapper.writeValueAsString(findAll);
        } catch (JsonProcessingException ex) {
            LOGGER.error("CMS PARTNER REQ BODY: ", ex);
        }
        String json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
        return json;
    }


    public int saveCMSPartner(CMSPartnerForm cmsPartnerForm) {
        int finalResponse = -1;
        try {
            String query = "INSERT INTO ega_partners(partner_name,acct_no,purpose,partner_type,partner_code,bot_acct,partner_status) VALUES(?,?,?,?,?,?,'A')";
            LOGGER.info("INSERT CMS Partner query: {}", query);
            finalResponse = this.jdbcTemplate.update(query, new Object[]{cmsPartnerForm.getPartner_name(), cmsPartnerForm.getAcct_no(), cmsPartnerForm.getPurpose(), cmsPartnerForm.getPartner_type(), cmsPartnerForm.getPartner_code(), cmsPartnerForm.getBot_acct()});
            LOGGER.info("final response by CMS Partner insert query [if 1 means SUCCESS]: {}", finalResponse);
        } catch (DataAccessException e) {
            e.printStackTrace();
        }
        return finalResponse;
    }

    public List<Map<String, Object>> getAccByControlNoAjax(String controlNo) {
        List<Map<String,Object>> finalRes = null;
        try{
            String sql = "SELECT * FROM tpb_partners_references WHERE reference= ?";
            finalRes = jdbcTemplate.queryForList(sql,controlNo);
        }catch (DataAccessException dae){
            LOGGER.info("Data access Exception... {}", dae);
        }
        return finalRes;
    }


    public PartnerReferences searchControlNumber(String searchValue) {

        PartnerReferences partnerReference=new PartnerReferences();
        try {
            partnerReference = jdbcTemplate.queryForObject("SELECT * FROM tpb_partners_references  WHERE reference = ?" , new RowMapper<PartnerReferences>() {
                @Override
                public PartnerReferences mapRow(ResultSet rs, int rowNum) throws SQLException {
                    PartnerReferences partnerReferences = new PartnerReferences();
                    partnerReferences.setAccountNumber(rs.getString("acct_no"));
                    partnerReferences.setFullName(rs.getString("full_name"));
                    partnerReferences.setPhoneNumber(rs.getString("phone_no"));
                    return partnerReferences;
                }
            },new Object[]{searchValue});
        }catch (Exception e){
            LOGGER.info("No record found in database for reference "+searchValue, e);
            partnerReference.setStatus("failed");
            return partnerReference;
        }

        return partnerReference;
    }

}
