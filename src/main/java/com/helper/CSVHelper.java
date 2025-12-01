package com.helper;

import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;


public class CSVHelper {
    public static List<String> TYPE = Arrays.asList("text/csv", "application/vnd.ms-excel");
    static String[] HEADERs = { "Batch", "Title"};

    public static boolean hasCSVFormat(MultipartFile file) {

        if (!TYPE.contains(file.getContentType())) {
            return false;
        }

        return true;
    }

    public static boolean hasJasperFormat(MultipartFile file) {

        if (!"application/octet-stream".equals(file.getContentType())) {
            return false;
        }

        return true;
    }

}
