/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.entities;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 *
 * @author melleji.mollel
 */
@Getter
@Setter
public class BatchTxnsEntry {
        private List<BatchTxnEntries> txn;

}
