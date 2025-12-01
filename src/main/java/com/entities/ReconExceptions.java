/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.entities;

import java.util.List;
import java.util.Map;

/**
 *
 * @author MELLEJI
 */
public class ReconExceptions {

    List<Map<String, Object>> exceptions;

    public List<Map<String, Object>> getExceptions() {
        return exceptions;
    }

    public void setExceptions(List<Map<String, Object>> exceptions) {
        this.exceptions = exceptions;
    }

    @Override
    public String toString() {
        return "ReconExceptions{" + "exceptions=" + exceptions + '}';
    }

}
