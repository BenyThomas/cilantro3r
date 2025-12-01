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
public class Mail {

    private String from;
    private String mailTo;
    private String cc;
    private String subject;
    private List<Object> attachments;
    private Map<String, Object> props;

    public String getFrom() {
        return from;
    }

    public String getCc() {
        return cc;
    }

    public void setCc(String cc) {
        this.cc = cc;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getMailTo() {
        return mailTo;
    }

    public void setMailTo(String mailTo) {
        this.mailTo = mailTo;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public List<Object> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<Object> attachments) {
        this.attachments = attachments;
    }

    public Map<String, Object> getProps() {
        return props;
    }

    public void setProps(Map<String, Object> props) {
        this.props = props;
    }

    @Override
    public String toString() {
        return "Mail{" + "from=" + from + ", mailTo=" + mailTo + ", cc=" + cc + ", subject=" + subject + ", attachments=" + attachments + ", props=" + props + '}';
    }

}
