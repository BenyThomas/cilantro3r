/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.service;

import com.helper.FileStorageException;
import java.io.File;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.springframework.util.StringUtils;

/**
 *
 * @author melleji.mollel
 */
@Service
public class FileService {

    @Value("${file.storage.location}")
    public String uploadDir;

    public void uploadFile(MultipartFile file) {

        try {
            Date date = new Date(System.currentTimeMillis());
            SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd");

            String filename = StringUtils.cleanPath(file.getOriginalFilename());
            filename = formatDate.format(date)+filename.toLowerCase().replaceAll(" ", "-");
            System.out.println("FILE NAMEEEEEEEET: "+filename);
            Path copyLocation = Paths.get(uploadDir + File.separator + formatDate.format(date)+StringUtils.cleanPath(file.getOriginalFilename()).toLowerCase().replace(" ", "-"));
            Files.copy(file.getInputStream(), copyLocation, StandardCopyOption.REPLACE_EXISTING);
//            
//            
//            file.getBytes();
//            file.getContentType();
        } catch (Exception e) {
            e.printStackTrace();
            throw new FileStorageException("Could not store file " + file.getOriginalFilename()
                    + ". Please try again!");
        }
    }
}
