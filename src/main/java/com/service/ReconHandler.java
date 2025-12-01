/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.service;

import com.entities.GatewayRefund;
import com.entities.GatewayRetry;
import com.entities.RetryRequest;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author MELLEJI
 */
@Service
public class ReconHandler {

    //Save the uploaded file to this folder
    private static String UPLOADED_FOLDER = "F://temp//";

    public String getRetryXml(String msisdn, String imsi, String account, String toaccount, String amount, String trans_type, String processcode, String msgid) {
        String iccXml = null;
        GatewayRetry retryXml = new GatewayRetry(msisdn, imsi, account, toaccount, amount, trans_type, processcode, msgid);
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(GatewayRetry.class);
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            StringWriter sw = new StringWriter();
            marshaller.marshal(retryXml, sw);
            iccXml = sw.toString();
            //System.out.println(iccXml);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        return iccXml;

    }

    public String getRefundXml(String msisdn, String imsi, String account, String toaccount, String amount, String trans_type, String processcode, String msgid) {
        String refundXml = null;
        GatewayRefund refundXml1 = new GatewayRefund(msisdn, imsi, account, toaccount, amount, trans_type, processcode, msgid);
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(GatewayRetry.class);
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            StringWriter sw = new StringWriter();
            marshaller.marshal(refundXml1, sw);
            refundXml = sw.toString();
            //System.out.println(iccXml);
        } catch (JAXBException e) {
        }
        return refundXml;

    }

    //save file
    public void saveUploadedFiles(MultipartFile uploadfile) throws IOException {
            try {
                String filename = uploadfile.getOriginalFilename();
                String directory = "D:\\dev\\software\\cilantro\\cilantro\\logs\\";
                String filepath = Paths.get(directory, filename).toString();
                try (BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(new File(filepath)))) {
                    stream.write(uploadfile.getBytes());
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
                //  return null;
            }
    }

}
