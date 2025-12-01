package com.helper;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MaiString {

    //regular expressss
    public static final Pattern USER_NAME_PATTERN = Pattern.compile("^([a-zA-Z])+([\\w@.]{4,40})+$");
    public static final Pattern INTEGER_PATTERN = Pattern.compile("^\\d{1,38}$");
    public static final Pattern INTEGER_DECIMAL_PATTERN = Pattern.compile("^\\d+(\\d+)*(\\.\\d+?)?$");
    public static final Pattern INTEGER_DECIMAL_NEGATIVE_PATTERN = Pattern.compile("^-?\\d+(\\d+)*(\\.\\d+?)?$");
    public static final Pattern MSISDN_255 = Pattern.compile("^255[1-9][0-9]{8}");
    private static final String INIT_VECTOR = "8765432112345678";
    private static final Logger LOGGER = LoggerFactory.getLogger(MaiString.class);
    private static final char[] ALPHANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private static final SecureRandom RNG = new SecureRandom();

    //regular expressss
    public static boolean isValidUserName(String item) {
        Matcher matcher = USER_NAME_PATTERN.matcher(item);
        return matcher.matches();
    }

    //regular expressss
    public static boolean isValidInteger(String item) {
        Matcher matcher = INTEGER_PATTERN.matcher(item);
        return matcher.matches();
    }

    public static boolean isValidIntegerOrDecimal(String item) {
        Matcher matcher = INTEGER_DECIMAL_PATTERN.matcher(item);
        return matcher.matches();
    }

    public static boolean isValidNegPosIntegerOrDecimal(String item) {
        Matcher matcher = INTEGER_DECIMAL_PATTERN.matcher(item);
        return matcher.matches();
    }

    public static boolean isValid255Msisdn(String item) {
        Matcher matcher = MSISDN_255.matcher(item);
        return matcher.matches();
    }

    public static String checkUssdProduct(String inp) {
        if (inp.contains("%23")) {
            inp.replaceAll("%23", "#");
        }
        if (!inp.contains("#") && inp.length() > 3 && inp.contains("*")) {
            inp = inp + ("#");
        }
        return inp;
    }

    public static Map<String, String> getQueryMap(String query) {
        String[] params = query.split("&");
        Map<String, String> map = new HashMap<>();
        for (String param : params) {
            if (param.contains("=")) {
                String name;
                String value;
                switch (param.split("=").length) {
                    case 2:
                        name = param.split("=")[0];
                        value = param.split("=")[1];
                        break;
                    case 1:
                        name = param.split("=")[0];
                        value = "-1";
                        break;
                    default:
                        name = "-1";
                        value = "-1";
                        break;
                }
                map.put(name, value);
            }
        }
        return map;
    }
  public static String formatCurrency(BigDecimal number) {
        DecimalFormat formatter = new DecimalFormat("#,###.00");
        return formatter.format(number);
    }
    public static String responseBodyBuilder(String body, String parameter1) {
        Map<String, String> map = getQueryMap(parameter1);
        Set<String> keys = map.keySet();
        for (String key : keys) {
            if (key.contains("CUSTOMER_NAME")) {
                body = body.replace("{CUSTOMER_NAME}", map.get("CUSTOMER_NAME"));
            }
            if (key.contains("PARTNER_NAME")) {
                body = body.replace("{PARTNER_NAME}", map.get("PARTNER_NAME"));
            }
            if (key.contains("BILL_MESSAGE")) {
                body = body.replace("{BILL_MESSAGE}", map.get("BILL_MESSAGE"));
            }
            if (key.contains("REF_NO")) {
                body = body.replace("{REF_NO}", map.get("REF_NO"));
            }
            if (key.contains("PARTNER_ACCT_NO")) {
                body = body.replace("{PARTNER_ACCT_NO}", map.get("PARTNER_ACCT_NO"));
            }
            if (key.contains("TO_PHOME_NO")) {
                body = body.replace("{TO_PHOME_NO}", map.get("TO_PHOME_NO"));
            }
            if (key.contains("A_ACCT_NO")) {
                body = body.replace("{A_ACCT_NO}", map.get("A_ACCT_NO"));
            }
            if (key.contains("B_ACCT_NO")) {
                body = body.replace("{B_ACCT_NO}", map.get("B_ACCT_NO"));
            }
            if (key.contains("A_AMOUNT")) {
                body = body.replace("{A_AMOUNT}", map.get("A_AMOUNT"));
            }
            if (key.contains("B_AMOUNT")) {
                body = body.replace("{B_AMOUNT}", map.get("B_AMOUNT"));
            }
            if (key.contains("PAYMENT_STRUCTURE_NAME")) {
                body = body.replace("{PAYMENT_STRUCTURE_NAME}", map.get("PAYMENT_STRUCTURE_NAME"));
            }
            if (key.contains("PRINCIPAL_AMOUNT")) {
                body = body.replace("{PRINCIPAL_AMOUNT}", map.get("PRINCIPAL_AMOUNT"));
            }
            if (key.contains("PAID_AMOUNT")) {
                body = body.replace("{PAID_AMOUNT}", map.get("PAID_AMOUNT"));
            }
            if (key.contains("REMAINING_AMOUNT")) {
                body = body.replace("{REMAINING_AMOUNT}", map.get("REMAINING_AMOUNT"));
            }
            if (key.contains("START_DATE")) {
                body = body.replace("{START_DATE}", map.get("START_DATE"));
            }
            if (key.contains("END_DATE")) {
                body = body.replace("{END_DATE}", map.get("END_DATE"));
            }
        }
        return body;
    }

    public static String getInputString(String varb, String parameter1, String parameter2) {
        String variable = "-1";
        Map<String, String> map = getQueryMap(parameter1 + "&" + parameter2);
        Set<String> keys = map.keySet();
        for (String key : keys) {
            if (key.contains(varb)) {
                variable = map.get(varb);
            }
        }
        return variable;
    }

    private static String randomAlphaNum4() {
        char[] buf = new char[4];
        for (int i = 0; i < 4; i++) buf[i] = ALPHANUM[RNG.nextInt(ALPHANUM.length)];
        return new String(buf);
    }

    public static String cbsReference(String nisogezeLoanId) {
        String id = nisogezeLoanId == null ? "" : nisogezeLoanId.trim().toUpperCase();
        return "TNS" + id + randomAlphaNum4(); // e.g. TNS999NS2102921AB3F
    }

    public static String prettyFormat(String input, int indent) {
        //xml make look good...
        try {
            Source xmlInput = new StreamSource(new StringReader(input));
            StringWriter stringWriter = new StringWriter();
            StreamResult xmlOutput = new StreamResult(stringWriter);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", indent);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(xmlInput, xmlOutput);
            return xmlOutput.getWriter().toString();
        } catch (IllegalArgumentException | TransformerException e) {
            throw new RuntimeException(e); // simple exception handling, please review it
        }
    }

    public static String getPAMobilePhone(String PhoneNo) {
        //255766101680 6DO5299CKVR to 255766101680
        return StringUtils.substringBetween(PhoneNo, "", " ");
    }

    public static String maskString(String strText, int start, int end, char maskChar)
            throws Exception {

        if (strText == null || strText.equals("")) {
            return "";
        }

        if (start < 0) {
            start = 0;
        }

        if (end > strText.length()) {
            end = strText.length();
        }

        if (start > end) {
            throw new Exception("End index cannot be greater than start index");
        }

        int maskLength = end - start;

        if (maskLength == 0) {
            return strText;
        }

        StringBuilder sbMaskString = new StringBuilder(maskLength);

        for (int i = 0; i < maskLength; i++) {
            sbMaskString.append(maskChar);
        }

        return strText.substring(0, start)
                + sbMaskString.toString()
                + strText.substring(start + maskLength);
    }

    public static List<Map<String, String>> buildListFromQuery(String parameter1) {
        Map<String, String> map = getQueryMap(parameter1);
        Set<String> keys = map.keySet();
        List<Map<String, String>> list = new ArrayList<>();
        for (String key : keys) {
            Map<String, String> row = new HashMap<>();
            row.put("item", key);
            row.put("value", map.get(key));
            list.add(row);
        }
        return list;
    }

    public static String randomString(int length) {
        return RandomStringUtils.randomAlphanumeric(length);
    }

    public static String capitailizeWord(String str) {
        StringBuilder s = new StringBuilder();
        // Declare a character of space 
        // To identify that the next character is the starting 
        // of a new word 
        char ch = ' ';
        for (int i = 0; i < str.length(); i++) {

            // If previous character is space and current 
            // character is not space then it shows that 
            // current letter is the starting of the word 
            if (ch == ' ' && str.charAt(i) != ' ') {
                s.append(Character.toUpperCase(str.charAt(i)));
            } else {
                s.append(str.charAt(i));
            }
            ch = str.charAt(i);
        }

        // Return the string with trimming 
        return s.toString().trim();
    }

    public static String formatMobileNo(String PhoneNo) {
        if (PhoneNo != null) {
            PhoneNo = PhoneNo.replace("+", "");
            PhoneNo = PhoneNo.replace(" ", "");
            if (PhoneNo.length() > 12) {
                PhoneNo = PhoneNo.substring(0, 12);
            }
            if (PhoneNo.length() == 9) {
                PhoneNo = "255" + PhoneNo;
            }
            if (PhoneNo.length() == 10) {
                PhoneNo = PhoneNo.replaceFirst("[0]", "255");
            }
        } else {
            PhoneNo = "NULL";
        }
        return PhoneNo;
    }

    public static String formatCurrency(double number) {
        DecimalFormat formatter = new DecimalFormat("#,###.00");
        return formatter.format(number);
    }

    public static String refFromTranDesc(String description) {
        description = description == null ? "REF:N/A DPS:" : description;
        return StringUtils.substringBetween(description, "REF:", " DPS:");
    }

    public static String encloseQuoteToCsvStr(String separator, String item) {
        List<String> lists = new ArrayList<>(Arrays.asList(item.split(separator)));
        return lists.stream().map(name -> ("'" + name + "'")).collect(Collectors.joining(","));
    }

    public static List<String> conventStrToList(String separator, String item) {
        return new ArrayList<>(Arrays.asList(item.split(separator)));
    }

    public static String encryptXapi(String key, String value) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(padText(key, 16, true).getBytes("UTF-8"), "AES"), new IvParameterSpec(INIT_VECTOR.getBytes("UTF-8")));
            return Base64.encodeBase64String(cipher.doFinal(value.getBytes()));
        } catch (UnsupportedEncodingException | InvalidAlgorithmParameterException | InvalidKeyException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException ex) {
            LOGGER.error("Exception:", ex);
        }
        return null;
    }




    private static String padText(String text, int length, boolean right) {
        return String.format("%" + (right ? "-" : "") + length + "s", text);
    }

    public static List<String> removeDuplicates(List<String> listWithDuplicates) {


    List<String> listWithoutDuplicates = new ArrayList<>(
      new HashSet<>(listWithDuplicates));


        // return the new list
        return listWithoutDuplicates;
    }

    public static String pad16LengthReference(String reference) {
        if (reference == null) {
            throw new IllegalArgumentException("Reference cannot be null");
        }

        int requiredLength = 16;

        if (reference.length() > requiredLength) {
            // Trim the reference to 16 characters
            return reference.substring(0, requiredLength);
        } else if (reference.length() < requiredLength) {
            // Pad with 'X' until the length is 16
            return String.format("%-" + requiredLength + "s", reference).replace(' ', 'X');
        }

        // Return as is if already 16 characters
        return reference;
    }

}
