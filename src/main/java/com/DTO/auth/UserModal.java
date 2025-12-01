/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.auth;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

public class UserModal implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String email;
    private String username;
    private String fullname;
    private boolean enabled;
    private String password;
    private int accountLocked;
    private boolean accountExpired;
    private String accountExpiredTime;
    private boolean credentialsExpired;
    private String credentialsExpiredTime;
    private String branchNo;
    private String branchName;
    private int isHeadOffice;
    private String lastLoginTime;
    private String partnerCode;
    private int domainId;
    private String domainName;
    private String msisdn;
    private String dob;
    private String createdBy;
    private String createdOn;
    private String deviceId;
    private String deviceStatus;
    private String ipAddress;
    private String limitPartnerAccts;
    private String allowedPartnerAccts;
    private String custId;
    private String clientProfileType;

    List<Map<String, Object>> roles;
    private List<Integer> roleId;
    private List<String> partnerAccounts;
    List<GrantedAuthority> authorities;
    private String isStaff;
    private String lang;
    private String noficationChannel;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(List<GrantedAuthority> authorities) {
        this.authorities = authorities;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof User) {
            return username.equals(((User) obj).getUsername());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return username != null ? username.hashCode() : 0;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getBranchNo() {
        return branchNo;
    }

    public void setBranchNo(String branchNo) {
        this.branchNo = branchNo;
    }

    public int getIsHeadOffice() {
        return isHeadOffice;
    }

    public void setIsHeadOffice(int isHeadOffice) {
        this.isHeadOffice = isHeadOffice;
    }

    public String getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(String lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public String getPartnerCode() {
        return partnerCode;
    }

    public void setPartnerCode(String partnerCode) {
        this.partnerCode = partnerCode;
    }

    public int getDomainId() {
        return domainId;
    }

    public void setDomainId(int domainId) {
        this.domainId = domainId;
    }

    public String getAccountExpiredTime() {
        return accountExpiredTime;
    }

    public void setAccountExpiredTime(String accountExpiredTime) {
        this.accountExpiredTime = accountExpiredTime;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public List<Map<String, Object>> getRoles() {
        return roles;
    }

    public void setRoles(List<Map<String, Object>> roles) {
        this.roles = roles;
    }

    public List<Integer> getRoleId() {
        return roleId;
    }

    public void setRoleId(List<Integer> roleId) {
        this.roleId = roleId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(String createdOn) {
        this.createdOn = createdOn;
    }

    public int isAccountLocked() {
        return accountLocked;
    }

    public void setAccountLocked(int accountLocked) {
        this.accountLocked = accountLocked;
    }

    public boolean isAccountExpired() {
        return accountExpired;
    }

    public void setAccountExpired(boolean accountExpired) {
        this.accountExpired = accountExpired;
    }

    public boolean isCredentialsExpired() {
        return credentialsExpired;
    }

    public void setCredentialsExpired(boolean credentialsExpired) {
        this.credentialsExpired = credentialsExpired;
    }

    public String getCredentialsExpiredTime() {
        return credentialsExpiredTime;
    }

    public void setCredentialsExpiredTime(String credentialsExpiredTime) {
        this.credentialsExpiredTime = credentialsExpiredTime;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getIsStaff() {
        return isStaff;
    }

    public void setIsStaff(String isStaff) {
        this.isStaff = isStaff;
    }

    public List<String> getPartnerAccounts() {
        return partnerAccounts;
    }

    public void setPartnerAccounts(List<String> partnerAccounts) {
        this.partnerAccounts = partnerAccounts;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceStatus() {
        return deviceStatus;
    }

    public void setDeviceStatus(String deviceStatus) {
        this.deviceStatus = deviceStatus;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getLimitPartnerAccts() {
        return limitPartnerAccts;
    }

    public void setLimitPartnerAccts(String limitPartnerAccts) {
        this.limitPartnerAccts = limitPartnerAccts;
    }

    public String getAllowedPartnerAccts() {
        return allowedPartnerAccts;
    }

    public void setAllowedPartnerAccts(String allowedPartnerAccts) {
        this.allowedPartnerAccts = allowedPartnerAccts;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getCustId() {
        return custId;
    }

    public void setCustId(String custId) {
        this.custId = custId;
    }

    public String getClientProfileType() {
        return clientProfileType;
    }

    public void setClientProfileType(String clientProfileType) {
        this.clientProfileType = clientProfileType;
    }

    @Override
    public String toString() {
        return "UserModal{" + "id=" + id + ", email=" + email + ", username=" + username + ", fullname=" + fullname + ", enabled=" + enabled + ", password=" + password + ", accountLocked=" + accountLocked + ", accountExpired=" + accountExpired + ", accountExpiredTime=" + accountExpiredTime + ", credentialsExpired=" + credentialsExpired + ", credentialsExpiredTime=" + credentialsExpiredTime + ", branchNo=" + branchNo + ", branchName=" + branchName + ", isHeadOffice=" + isHeadOffice + ", lastLoginTime=" + lastLoginTime + ", partnerCode=" + partnerCode + ", domainId=" + domainId + ", domainName=" + domainName + ", msisdn=" + msisdn + ", dob=" + dob + ", createdBy=" + createdBy + ", createdOn=" + createdOn + ", deviceId=" + deviceId + ", deviceStatus=" + deviceStatus + ", ipAddress=" + ipAddress + ", limitPartnerAccts=" + limitPartnerAccts + ", allowedPartnerAccts=" + allowedPartnerAccts + ", custId=" + custId + ", clientProfileType=" + clientProfileType + ", roles=" + roles + ", roleId=" + roleId + ", partnerAccounts=" + partnerAccounts + ", authorities=" + authorities + ", isStaff=" + isStaff + ", lang=" + lang + '}';
    }

    public String getNoficationChannel() {
        return noficationChannel;
    }

    public void setNoficationChannel(String noficationChannel) {
        this.noficationChannel = noficationChannel;
    }

 
   
}
