/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.service;

import com.entities.Mail;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

/**
 *
 * @author MELLEJI
 */
@Service
public class EmailSenderService {

    @Autowired
    private JavaMailSender emailSender;

    @Autowired
    private SpringTemplateEngine templateEngine;

    public void sendEmail(Mail mail, List<Map<String, Object>> transactions, String mno, String ttype, String senderName) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());
            final Context context = new Context();
            context.setVariables(mail.getProps());
            context.setVariable("transactions", transactions);
            context.setVariable("username", senderName);
            context.setVariable("ttype", ttype);
            context.setVariable("mno", mno);
            String html = this.templateEngine.process("settings/emailTemplate", context);

            helper.setTo(InternetAddress.parse(mail.getMailTo()));
            helper.setCc(InternetAddress.parse(mail.getCc()));
            helper.setText(html, true);
            helper.setSubject(mail.getSubject());
            helper.setFrom(mail.getFrom());
            System.out.println("PREPARING TO SEND TEST EMAIL");
            this.emailSender.send(message);
            System.out.println("EMAIL SENT AS TEST EMAIL");
        } catch (MessagingException ex) {
            Logger.getLogger(EmailSenderService.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendEmailTest() {
        //            MimeMessage message = emailSender.createMimeMessage();
//            MimeMessageHelper helper = new MimeMessageHelper(message,
//                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
//                    StandardCharsets.UTF_8.name());
//            final Context context = new Context();
//            context.setVariables(mail.getProps());
//            context.setVariable("transactions", transactions);
//            context.setVariable("ttype", ttype);
//            context.setVariable("mno", mno);
//            String html = this.templateEngine.process("settings/emailTemplate", context);
//
//            System.out.println("{MAIL SERVICE: }");
//            helper.setTo(InternetAddress.parse(mail.getMailTo()));
//            helper.setCc(InternetAddress.parse(mail.getCc()));
//            helper.setText(html, true);
//            helper.setSubject(mail.getSubject());
//            helper.setFrom(mail.getFrom());
//            this.emailSender.send(message);

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo("mellejlelya2010@gmail.com");
        msg.setSubject("Testing from RECON");
        msg.setText("Hello World \n Spring Boot Email");
        System.out.println("PREPARING TO SEND TEST EMAIL");
        emailSender.send(msg);
        System.out.println("EMAIL SENT AS TEST EMAIL");

    }
}
