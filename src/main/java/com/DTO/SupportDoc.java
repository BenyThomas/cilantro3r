/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.online.core.request;

import java.io.Serializable;

/**
 * *
 * @author samichael
 */
public class SupportDoc implements Serializable {

    private String fileName;
    private String txnId;
    private String recStatus;
    protected String fileSize;
    protected String fileExt;
    private byte[] fileBlob;
    private String fileBase64;
    private String acsToken;
    private String id;
    private String logDate;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getTxnId() {
        return txnId;
    }

    public void setTxnId(String txnId) {
        this.txnId = txnId;
    }

    public String getRecStatus() {
        return recStatus;
    }

    public void setRecStatus(String recStatus) {
        this.recStatus = recStatus;
    }

    public String getFileSize() {
        return fileSize;
    }

    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileExt() {
        return fileExt;
    }

    public void setFileExt(String fileExt) {
        this.fileExt = fileExt;
    }

    public byte[] getFileBlob() {
        return fileBlob;
    }

    public void setFileBlob(byte[] fileBlob) {
        this.fileBlob = fileBlob;
    }

    public String getAcsToken() {
        return acsToken;
    }

    public void setAcsToken(String acsToken) {
        this.acsToken = acsToken;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLogDate() {
        return logDate;
    }

    public void setLogDate(String logDate) {
        this.logDate = logDate;
    }

    public String getFileBase64() {
        return fileBase64;
    }

    public void setFileBase64(String fileBase64) {
        this.fileBase64 = fileBase64;
    }

    @Override
    public String toString() {
        return "SupportDoc{" + "fileName=" + fileName + ", txnId=" + txnId + ", recStatus=" + recStatus + ", fileSize=" + fileSize + ", fileExt=" + fileExt + ", fileBlob=" + fileBlob + ", fileBase64=" + fileBase64 + ", acsToken=" + acsToken + ", id=" + id + ", logDate=" + logDate + '}';
    }

}
