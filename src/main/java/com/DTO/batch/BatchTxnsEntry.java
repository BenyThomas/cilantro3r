/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.batch;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author melleji.mollel
 */
@Getter
@Setter
public class BatchTxnsEntry {

    private List<BatchTxnEntries> txn;

}
