/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.service;

import com.config.SYSENV;
import com.helper.SignRequest;
import com.prowidesoftware.swift.io.parser.MxParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author melleji.mollel
 */
@Service
public class BotVpnService {

    @Autowired
    SignRequest sign;
    @Autowired
    SYSENV systemVariables;

    public String  parseSwiftMessage(String Message) throws Exception {
        String eftMessageSinged = Message.split("\\^")[1];
        String fileName = Message.split("\\^")[0];
        String eftMessageContent = eftMessageSinged.split("\\|")[0];
        String signature = eftMessageSinged.split("\\|")[1];
        boolean isEftMessageValid = sign.verifySignature(signature, eftMessageContent, systemVariables.PUBLIC_EFT_TACH_KEYPASS, systemVariables.PUBLIC_EFT_TACH_KEY_ALIAS, systemVariables.PUBLIC_EFT_TACH_KEY_FILE_PATH);
        System.out.println("IS MESSAGE VALID? " + isEftMessageValid);
        if (isEftMessageValid) {
            MxParser swiftMessage = new MxParser(eftMessageContent);
            if (swiftMessage.analyzeMessage().getDocumentNamespace().equals("urn:iso:std:iso:20022:tech:xsd:pacs.008.001.02")) {
//                processPacs00800201Incoming(eftMessageContent, fileName, false);
            } else if (swiftMessage.analyzeMessage().getDocumentNamespace().equals("urn:iso:std:iso:20022:tech:xsd:pacs.002.001.03")) {
//                processPacs00200103Incoming(eftMessageContent, fileName);
            } else if (swiftMessage.analyzeMessage().getDocumentNamespace().equals("urn:iso:std:iso:20022:tech:xsd:pacs.004.001.02")) {
//                processPacs00400102(eftMessageContent, fileName);
            }
        } else {
//            processPacs00800201Incoming(eftMessageContent, fileName, true);
            System.out.println("RECEIVED FILE: " + fileName + " signature cannot be verified!!!!!!");
        }
        return null;
    }
}
