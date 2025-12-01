/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.entities;

import org.springframework.security.core.GrantedAuthority;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 *
 * @author MELLEJI
 */
public class Modules implements Serializable {

    private String name;
    private String id;
    private String url;
    private String icon;

    List<Map<String, Object>> getPermissions;
    List<Map<String, Object>> getSubPermissions;
    List<Map<String, Object>> getOperationsPermissions;
    List<Map<String, Object>> paymentsPermissions;
    List<Map<String, Object>> paymentsModules;

    List<GrantedAuthority> authorities;
    List<Map<String, Object>> kycPermissions;

    public List<Map<String, Object>> getPaymentsPermissions() {
        return paymentsPermissions;
    }

    public void setPaymentsPermissions(List<Map<String, Object>> paymentsPermissions) {
        this.paymentsPermissions = paymentsPermissions;
    }

    public List<Map<String, Object>> getGetPermissions() {
        return getPermissions;
    }

    public void setGetPermissions(List<Map<String, Object>> getPermissions) {
        this.getPermissions = getPermissions;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public List<Map<String, Object>> getGetSubPermissions() {
        return getSubPermissions;
    }

    public void setGetSubPermissions(List<Map<String, Object>> getSubPermissions) {
        this.getSubPermissions = getSubPermissions;
    }

    public List<Map<String, Object>> getGetOperationsPermissions() {
        return getOperationsPermissions;
    }

    public void setGetOperationsPermissions(List<Map<String, Object>> getOperationsPermissions) {
        this.getOperationsPermissions = getOperationsPermissions;
    }

    public List<Map<String, Object>> getPaymentsModules() {
        return paymentsModules;
    }

    public void setPaymentsModules(List<Map<String, Object>> paymentsModules) {
        this.paymentsModules = paymentsModules;
    }

    @Override
    public String toString() {
        return "Modules{" + "name=" + name + ", id=" + id + ", url=" + url + ", icon=" + icon + ", getPermissions=" + getPermissions + ", getSubPermissions=" + getSubPermissions + ", getOperationsPermissions=" + getOperationsPermissions + ", paymentsPermissions=" + paymentsPermissions + ", paymentsModules=" + paymentsModules + '}';
    }

    public List<GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(List<GrantedAuthority> authorities) {
        this.authorities = authorities;
    }




    public List<Map<String, Object>> getKycPermissions() {
        return kycPermissions;
    }

    public void setKycPermissions(List<Map<String, Object>> kycPermissions) {
        this.kycPermissions = kycPermissions;
    }
}
