/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.entities;

import jakarta.validation.constraints.NotBlank;

/**
 *
 * @author MELLEJI
 */
public class RoleForm {

    @NotBlank(message = "Role name Is required")
    private String roleName;
    @NotBlank(message = "Description is Required")
    private String description;

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "RoleForm{" + "roleName=" + roleName + ", description=" + description + '}';
    }
}
