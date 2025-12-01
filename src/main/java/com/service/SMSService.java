package com.service;

import com.dao.sms.SMSDTO;
import com.dao.kyc.response.ors.ResponseDTO;

public interface SMSService {
    ResponseDTO<?> sendSMS(SMSDTO smsDTO);
    String sendXMLRequest(String messageBody, String smsGatewayUri);
}
