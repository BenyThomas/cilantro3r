/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.helper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 *
 * @author melleji.mollel
 */
public class StringUtils {

    public static String getMTLineSplitter(String name) {
        String description1 = "";
        String description2 = "";
        String description3 = "";
        String description4 = "";
        String description = name;
        String descriptionArray[] = name.split("(?<=\\G.{30})");
        switch (descriptionArray.length) {
            case 4:
                description1 = descriptionArray[0].trim();
                description2 = descriptionArray[1].trim();
                description3 = descriptionArray[2].trim();
                description4 = descriptionArray[3].trim();
                description = description1 + "\n" + description2 + "\n" + description3 + "\n" + description4;
                break;
            case 3:
                description1 = descriptionArray[0].trim();
                description2 = descriptionArray[1].trim();
                description3 = descriptionArray[2].trim();
                description = description1 + "\n" + description2 + "\n" + description3;
                break;
            case 2:
                description1 = descriptionArray[0].trim();
                description2 = descriptionArray[1].trim();
                description = description1 + "\n" + description2;
                break;
            case 1:
                description1 = descriptionArray[0].trim();
                description = description1;
                break;
            default:
                break;
        }
        return description;

    }

    public static String getMTLineSplitterLimitedTo70Chars(String name) {
        String description1 = "";
        String description2 = "";
        String description = name;
        String descriptionArray[] = name.split("(?<=\\G.{35})");
        switch (descriptionArray.length) {
            case 4:
                description1 = descriptionArray[0].trim();
                description2 = descriptionArray[1].trim();
                description = description1 + "\n" + description2;
                break;
            case 3:
                description1 = descriptionArray[0].trim();
                description2 = descriptionArray[1].trim();
                description = description1 + "\n" + description2;
                break;
            case 2:
                description1 = descriptionArray[0].trim();
                description2 = descriptionArray[1].trim();
                description = description1 + "\n" + description2;
                break;
            case 1:
                description1 = descriptionArray[0].trim();
                description = description1;
                break;
            default:
                break;
        }
        return description;

    }

    public static String getMTLineSplitterLimitedTo35Chars(String name) {
        String description1 = "";
        String description2 = "";
        String description = name;
        String descriptionArray[] = name.split("(?<=\\G.{33})");
        switch (descriptionArray.length) {
            case 4:
                description1 = descriptionArray[0].trim();
                description = description1;
                break;
            case 3:
                description1 = descriptionArray[0].trim();
                description = description1;
                break;
            case 2:
                description1 = descriptionArray[0].trim();
                description = description1;
                break;
            case 1:
                description1 = descriptionArray[0].trim();
                description = description1;
                break;
            default:
                break;
        }
        return description;

    }
    public static String generateIsoTransactionReference() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        String uuidPart = UUID.randomUUID().toString().substring(0, 8); // Optional unique suffix
        return timestamp + "-" + uuidPart;
    }

}
