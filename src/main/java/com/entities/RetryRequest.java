/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.entities;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author MELLEJI
 */
public class RetryRequest {
@NotBlank(message = "Please enter a Reason")
    private String reason;

    private MultipartFile file;

    @NotEmpty
    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public MultipartFile getFile() {
        return file;
    }

    public void setFiles(MultipartFile file) {
        this.file = file;
    }

    @Override
    public String toString() {
        return "RetryRequest{" + "reason=" + reason + ", file=" + file + '}';
    }

}
