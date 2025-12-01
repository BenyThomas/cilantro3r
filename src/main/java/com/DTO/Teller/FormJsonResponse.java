/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.Teller;

/**
 *
 * @author melleji.mollel
 */
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

/**
 *
 * @author MELLEJI
 */
@Getter
@Setter
@ToString
public class FormJsonResponse {

    private RTGSTransferForm rtgsTransferForm;
    private boolean validated;
    private String jsonString;
    private Object errorMessages;
}
