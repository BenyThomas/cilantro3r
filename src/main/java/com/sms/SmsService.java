/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sms;

import com.helper.MaiString;
import com.repository.IbankRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author samichael
 */
@Service
public class SmsService {

    @Autowired
    SmsRepository smsRepository;

    private static final Logger LOGGER = LoggerFactory.getLogger(SmsService.class);

    public String smsBodyBuilder(int msgId, String lang, String parameters, String senderChannel) {
        LOGGER.info("Parameters: {}",parameters);
        String readySMS = null;
        String rawSmsBody = "-1";
        SmsBody smsBody = smsRepository.getSmsBody(msgId, lang, senderChannel);
        if (smsBody != null) {
            rawSmsBody = smsBody.getMsg();
        }
        readySMS = MaiString.responseBodyBuilder(rawSmsBody, parameters);
        return readySMS;
    }

}
