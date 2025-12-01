/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.service;

import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 *
 * @author samichael
 */
@Service
public class MailService {

    @Autowired
    private JavaMailSender tpbMailSender;

   
    public void sendHtmlEmail(String htmlBody,Map<String, Object> form, List<Map<String, Object>> attachments) throws MessagingException {
        // Prepare message using a Spring helper
        final MimeMessage mimeMessage = this.tpbMailSender.createMimeMessage();
        mimeMessage.addHeader("Content-Type", "text/html; charset=UTF-8");
        mimeMessage.setSentDate(new Date());
        final MimeMessageHelper message = new MimeMessageHelper(mimeMessage, true, "UTF-8"); // true = multipart
        message.setSubject(form.get("mailSubject") + "");
        message.setFrom(form.get("mailFrom") + "");
        message.setTo(InternetAddress.parse(form.get("mailTo") + ""));
        if (form.get("mailBCC") != null) {
            message.setBcc(InternetAddress.parse(form.get("mailBCC") + ""));
        }
        if (form.get("mailCC") != null) {
            message.setCc(InternetAddress.parse(form.get("mailCC") + ""));
        }
        for (Map<String, Object> attachment : attachments) {
            message.addAttachment(String.valueOf(attachment.get("fileName")), new ByteArrayResource(((ByteArrayOutputStream) attachment.get("fileByteStream")).toByteArray(), String.valueOf(attachment.get("fileMime"))));
        }
        // Create the HTML body using Thymeleaf
        message.setText(htmlBody, true); // true = isHtml
        // Send mail
        this.tpbMailSender.send(mimeMessage);
    }

}
