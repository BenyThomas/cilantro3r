///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package com.helper;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.io.UnsupportedEncodingException;
//import java.math.BigInteger;
//import java.security.InvalidAlgorithmParameterException;
//import java.security.InvalidKeyException;
//import java.security.MessageDigest;
//import java.security.NoSuchAlgorithmException;
//import java.text.SimpleDateFormat;
//import java.util.Calendar;
//import java.util.Date;
//import java.util.Locale;
//import java.util.UUID;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import javax.crypto.Cipher;
//import javax.crypto.NoSuchPaddingException;
//import javax.crypto.spec.IvParameterSpec;
//import javax.crypto.spec.SecretKeySpec;
//import org.springframework.stereotype.Component;
//import sun.misc.BASE64Decoder;
//import sun.misc.BASE64Encoder;
//
///**
// *
// * @author Melleji Mollel
// */
//@Component
//public class LicenseUtils {
//
//    public static final String iv = "0123456789123456";
//    private static final String secrete = "CILANTRO";
//
//    public String getHardwareId() {
//        Logger.getLogger(LicenseUtils.class.getName()).log(Level.FINE, "start", "");
//        if (System.getProperty("os.name").toLowerCase(Locale.US).startsWith("windows")) {
//            Logger.getLogger(LicenseUtils.class.getName()).log(Level.FINE, "start " + System.getProperty("os.name"), "");
//            BufferedReader bufferedReader = null;
//            InputStreamReader inputStreamReader = null;
//            try {
//                Process process = Runtime.getRuntime().exec("cmd /C dir c:\\");
//                inputStreamReader = new InputStreamReader(process.getInputStream(), "UTF-8");
//                (bufferedReader = new BufferedReader(inputStreamReader)).readLine();
//                String str;
//                if ((str = bufferedReader.readLine()) != null && (str = str.substring(str.length() - 9, str.length())).length() > 1) {
//                    try {
//                        UUID uUID = UUID.nameUUIDFromBytes(str.getBytes("UTF-8"));
//                        return "3" + uUID.toString();
//                    } catch (UnsupportedEncodingException unsupportedEncodingException) {
//                        Logger.getLogger(LicenseUtils.class.getName()).log(Level.FINE, "MSG145" + System.getProperty("os.name"), "");
//                    }
//                }
//                return null;
//            } catch (Exception exception1) {
//                Logger.getLogger(LicenseUtils.class.getName()).log(Level.FINE, "MSG106", exception1);
//            } finally {
//            }
//        } else {
//            if (System.getProperty("os.name").toLowerCase(Locale.US).startsWith("linux")) {
//                Logger.getLogger(LicenseUtils.class.getName()).log(Level.FINE, "start " + System.getProperty("os.name"), "");
//                String str;
//                if ((str = o()) == null) {
//                    str = p();
//                }
//                if (str == null) {
//                    str = q();
//                }
//                if (str == null) {
//                    str = r();
//                }
//                if (str == null) {
//                    str = s();
//                }
//                if (str == null) {
//                    str = t();
//                }
//                if (str == null) {
//                    str = u();
//                }
//                if (str == null) {
//                    str = v();
//                }
//                return str;
//            }
//            if (System.getProperty("os.name").toLowerCase(Locale.US).startsWith("mac")) {
//                return w();
//            }
//        }
//        return null;
//    }
//
//    private static String o() {
//        BufferedReader bufferedReader = null;
//        try {
//            Process process = Runtime.getRuntime().exec("ls -l /dev/disk/by-uuid/ | grep sda1;ls -l /dev/disk/by-uuid/ | grep hda1;ls -l /dev/disk/by-uuid/ | grep xvda1");
//            bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
//            String str2 = null;
//            String str1;
//            while ((str1 = bufferedReader.readLine()) != null) {
//                if (str1.indexOf("sda1") != -1 || str1.indexOf("hda1") != -1 || (str1.indexOf("xvda1") != -1 && str1.substring(str1.indexOf("->") - 37, str1.indexOf("->") - 1).length() > 30)) {
//                    str2 = str1;
//                }
//            }
//            if (str2 != null && str2.length() > 40 && (str2 = str2.substring(str2.indexOf("->") - 37, str2.indexOf("->") - 1)).length() > 1) {
//                if (str2.indexOf(":") != -1) {
//                    str2 = str2.substring(str2.length() - 16, str2.length());
//                }
//                if (str2.indexOf(":") != -1) {
//                    str2 = str2.substring(str2.length() - 9, str2.length());
//                }
//                UUID uUID = UUID.nameUUIDFromBytes(str2.getBytes("UTF-8"));
//                return "3" + uUID.toString();
//            }
//        } catch (Exception exception) {
//            Logger.getLogger(LicenseUtils.class.getName()).log(Level.FINE, "MSG129", exception);
//        } finally {
////      try {
////        if (iOException != null)
////          iOException.close(); 
////      } catch (IOException iOException1) {
////        Logger.getLogger(a.class.getName()).log(Level.FINE, "MSG130", iOException1);
////      } 
//        }
//        return null;
//    }
//
//    private static String p() {
//        BufferedReader bufferedReader = null;
//        try {
//            Process process = Runtime.getRuntime().exec("ls -l /dev/disk/by-uuid/ | grep c0d0p1;ls -l /dev/disk/by-uuid/ | grep vda1");
//            bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
//            String str2 = null;
//            String str1;
//            while ((str1 = bufferedReader.readLine()) != null) {
//                if (str1.indexOf("c0d0p1") != -1 || (str1.indexOf("vda1") != -1 && str1.substring(str1.indexOf("->") - 37, str1.indexOf("->") - 1).length() > 30)) {
//                    str2 = str1;
//                }
//            }
//            if (str2 != null && str2.length() > 40 && (str2 = str2.substring(str2.indexOf("->") - 37, str2.indexOf("->") - 1)).length() > 1) {
//                UUID uUID = UUID.nameUUIDFromBytes(str2.getBytes("UTF-8"));
//                return "3" + uUID.toString();
//            }
//        } catch (Exception exception) {
//            Logger.getLogger(LicenseUtils.class.getName()).log(Level.FINE, "MSG131", exception);
//        } finally {
////      try {
////        if (iOException != null)
////          iOException.close(); 
////      } catch (IOException iOException1) {
////        Logger.getLogger(a.class.getName()).log(Level.FINE, "MSG132", iOException1);
////      } 
//        }
//        return null;
//    }
//
//    private static String q() {
//        BufferedReader bufferedReader = null;
//        try {
//            Process process = Runtime.getRuntime().exec("ls -l /dev/disk/by-uuid/ | grep mmcblk0p1;ls -l /dev/disk/by-uuid/ | grep xvda2");
//            bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
//            String str2 = null;
//            String str1;
//            while ((str1 = bufferedReader.readLine()) != null) {
//                if (str1.indexOf("mmcblk0p1") != -1 || (str1.indexOf("xvda2") != -1 && str1.substring(str1.indexOf("->") - 37, str1.indexOf("->") - 1).length() > 30)) {
//                    str2 = str1;
//                }
//            }
//            if (str2 != null && str2.length() > 40 && (str2 = str2.substring(str2.indexOf("->") - 37, str2.indexOf("->") - 1)).length() > 1) {
//                if (str2.indexOf(":") != -1) {
//                    str2 = str2.substring(str2.length() - 16, str2.length());
//                }
//                UUID uUID = UUID.nameUUIDFromBytes(str2.getBytes("UTF-8"));
//                return "3" + uUID.toString();
//            }
//        } catch (Exception exception) {
//            Logger.getLogger(LicenseUtils.class.getName()).log(Level.FINE, "MSG129", exception);
//        } finally {
////      try {
////        if (iOException != null)
////          iOException.close(); 
////      } catch (IOException iOException1) {
////        Logger.getLogger(a.class.getName()).log(Level.FINE, "MSG130", iOException1);
////      } 
//        }
//        return null;
//    }
//
//    private static String r() {
//        BufferedReader bufferedReader = null;
//        try {
//            Process process = Runtime.getRuntime().exec("ls -l /dev/disk/by-uuid/ | grep vda2");
//            bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
//            String str2 = null;
//            String str1;
//            while ((str1 = bufferedReader.readLine()) != null) {
//                if (str1.indexOf("vda2") != -1 && str1.substring(str1.indexOf("->") - 37, str1.indexOf("->") - 1).length() > 30) {
//                    str2 = str1;
//                }
//            }
//            if (str2 != null && str2.length() > 40 && (str2 = str2.substring(str2.indexOf("->") - 37, str2.indexOf("->") - 1)).length() > 1) {
//                if (str2.indexOf(":") != -1) {
//                    str2 = str2.substring(str2.length() - 16, str2.length());
//                }
//                UUID uUID = UUID.nameUUIDFromBytes(str2.getBytes("UTF-8"));
//                return "3" + uUID.toString();
//            }
//        } catch (Exception exception) {
//            Logger.getLogger(LicenseUtils.class.getName()).log(Level.FINE, "MSG129", exception);
//        } finally {
////            try {
////                if (iOException != null) {
////                    iOException.close();
////                }
////            } catch (IOException iOException1) {
////                Logger.getLogger(a.class.getName()).log(Level.FINE, "MSG130", iOException1);
////            }
//        }
//        return null;
//    }
//
//    private static String s() {
//        BufferedReader bufferedReader = null;
//        try {
//            Process process = Runtime.getRuntime().exec("ls -l /dev/disk/by-uuid/ | grep sda2");
//            bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
//            String str2 = null;
//            String str1;
//            while ((str1 = bufferedReader.readLine()) != null) {
//                if (str1.indexOf("sda2") != -1 && str1.substring(str1.indexOf("->") - 37, str1.indexOf("->") - 1).length() > 30) {
//                    str2 = str1;
//                }
//            }
//            if (str2 != null && str2.length() > 40 && (str2 = str2.substring(str2.indexOf("->") - 37, str2.indexOf("->") - 1)).length() > 1) {
//                if (str2.indexOf(":") != -1) {
//                    str2 = str2.substring(str2.length() - 16, str2.length());
//                }
//                UUID uUID = UUID.nameUUIDFromBytes(str2.getBytes("UTF-8"));
//                return "3" + uUID.toString();
//            }
//        } catch (Exception exception) {
//            Logger.getLogger(LicenseUtils.class.getName()).log(Level.FINE, "MSG129", exception);
//        } finally {
////            try {
////                if (iOException != null) {
////                    iOException.close();
////                }
////            } catch (IOException iOException1) {
////                Logger.getLogger(a.class.getName()).log(Level.FINE, "MSG130", iOException1);
////            }
//        }
//        return null;
//    }
//
//    private static String t() {
//        BufferedReader bufferedReader = null;
//        try {
//            Process process = Runtime.getRuntime().exec("ls -l /dev/disk/by-uuid/ | grep dm-1");
//            bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
//            String str2 = null;
//            String str1;
//            while ((str1 = bufferedReader.readLine()) != null) {
//                if (str1.indexOf("dm-1") != -1 && str1.substring(str1.indexOf("->") - 37, str1.indexOf("->") - 1).length() > 30) {
//                    str2 = str1;
//                }
//            }
//            if (str2 != null && str2.length() > 40 && (str2 = str2.substring(str2.indexOf("->") - 37, str2.indexOf("->") - 1)).length() > 1) {
//                if (str2.indexOf(":") != -1) {
//                    str2 = str2.substring(str2.length() - 16, str2.length());
//                }
//                UUID uUID = UUID.nameUUIDFromBytes(str2.getBytes("UTF-8"));
//                return "3" + uUID.toString();
//            }
//        } catch (Exception exception) {
//            Logger.getLogger(LicenseUtils.class.getName()).log(Level.FINE, "MSG129", exception);
//        } finally {
////            try {
////                if (iOException != null) {
////                    iOException.close();
////                }
////            } catch (IOException iOException1) {
////                Logger.getLogger(a.class.getName()).log(Level.FINE, "MSG130", iOException1);
////            }
//        }
//        return null;
//    }
//
//    private static String u() {
//        BufferedReader bufferedReader = null;
//        try {
//            Process process = Runtime.getRuntime().exec("ls -l /dev/disk/by-uuid/ | grep dm-0");
//            bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
//            String str2 = null;
//            String str1;
//            while ((str1 = bufferedReader.readLine()) != null) {
//                if (str1.indexOf("dm-0") != -1 && str1.substring(str1.indexOf("->") - 37, str1.indexOf("->") - 1).length() > 30) {
//                    str2 = str1;
//                }
//            }
//            if (str2 != null && str2.length() > 40 && (str2 = str2.substring(str2.indexOf("->") - 37, str2.indexOf("->") - 1)).length() > 1) {
//                if (str2.indexOf(":") != -1) {
//                    str2 = str2.substring(str2.length() - 16, str2.length());
//                }
//                UUID uUID = UUID.nameUUIDFromBytes(str2.getBytes("UTF-8"));
//                return "3" + uUID.toString();
//            }
//        } catch (Exception exception) {
//            Logger.getLogger(LicenseUtils.class.getName()).log(Level.FINE, "MSG129", exception);
//        } finally {
////            try {
////                if (iOException != null) {
////                    iOException.close();
////                }
////            } catch (IOException iOException1) {
////                Logger.getLogger(a.class.getName()).log(Level.FINE, "MSG130", iOException1);
////            }
//        }
//        return null;
//    }
//
//    private static String v() {
//        BufferedReader bufferedReader = null;
//        try {
//            Process process = Runtime.getRuntime().exec("ls -l /dev/disk/by-uuid/ | grep nvme0n1p1;ls -l /dev/disk/by-uuid/ | grep nvme0n1p2");
//            bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
//            String str2 = null;
//            String str1;
//            while ((str1 = bufferedReader.readLine()) != null) {
//                if (str1.indexOf("nvme0n1p") != -1 && str1.substring(str1.indexOf("->") - 37, str1.indexOf("->") - 1).length() > 30) {
//                    str2 = str1;
//                }
//            }
//            if (str2 != null && str2.length() > 40 && (str2 = str2.substring(str2.indexOf("->") - 37, str2.indexOf("->") - 1)).length() > 1) {
//                if (str2.indexOf(":") != -1) {
//                    str2 = str2.substring(str2.length() - 16, str2.length());
//                }
//                UUID uUID = UUID.nameUUIDFromBytes(str2.getBytes("UTF-8"));
//                return "3" + uUID.toString();
//            }
//        } catch (Exception exception) {
//            Logger.getLogger(LicenseUtils.class.getName()).log(Level.FINE, "MSG129", exception);
//        } finally {
////            try {
////                if (iOException != null) {
////                    iOException.close();
////                }
////            } catch (IOException iOException1) {
////                Logger.getLogger(a.class.getName()).log(Level.FINE, "MSG130", iOException1);
////            }
//        }
//        return null;
//    }
//
//    private static String w() {
//        BufferedReader bufferedReader = null;
//        try {
//            Process process = Runtime.getRuntime().exec("diskutil info /");
//            bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
//            String str2 = null;
//            String str1;
//            while ((str1 = bufferedReader.readLine()) != null) {
//                if (str1.indexOf("Volume UUID:") != -1) {
//                    str2 = str1;
//                    break;
//                }
//            }
//            if (str2 != null && (str2 = str2.substring(str2.length() - 36, str2.length())).length() > 1) {
//                UUID uUID = UUID.nameUUIDFromBytes(str2.getBytes("UTF-8"));
//                return "3" + uUID.toString();
//            }
//        } catch (IOException iOException1) {
//            Logger.getLogger(LicenseUtils.class.getName()).log(Level.FINE, "MSG133", iOException1);
//        } finally {
////            try {
////                if (iOException != null) {
////                    iOException.close();
////                }
////            } catch (IOException iOException1) {
////                Logger.getLogger(a.class.getName()).log(Level.FINE, "MSG134", iOException1);
////            }
//        }
//        return null;
//    }
//
//    public String getExpiredate() {
//        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
//        Calendar cal = Calendar.getInstance();
//        Date today = cal.getTime();
//        cal.add(Calendar.YEAR, 1); // to get previous year add -1
//        Date date = cal.getTime();
//        return encrypt(formatter.format(date), iv, secrete);
//    }
//
//    public String generateLicenseKey() {
//        String digest = encrypt(encrypt(getHardwareId(), iv, secrete), iv, secrete) + "__" + encrypt(getExpiredate(), iv, secrete);
//        return encrypt(digest, iv, secrete);
//    }
//
//    public String encrypt(final String dataToEncrypt, final String initialVector, final String secretKey) {
//        String encryptedData = null;
//        try {
//            // Initialize the cipher
//            final Cipher cipher = initCipher(Cipher.ENCRYPT_MODE, initialVector, secretKey);
//            // Encrypt the data
//            final byte[] encryptedByteArray = cipher.doFinal(dataToEncrypt.getBytes());
//            // Encode using Base64
//            encryptedData = (new BASE64Encoder()).encode(encryptedByteArray);
//        } catch (Exception e) {
//            System.err.println("Problem encrypting the data");
//            e.printStackTrace();
//        }
//        return encryptedData;
//    }
//
//    private Cipher initCipher(final int mode, final String initialVectorString, final String secretKey)
//            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
//        final SecretKeySpec skeySpec = new SecretKeySpec(md5(secretKey).getBytes(), "AES");
//        final IvParameterSpec initialVector = new IvParameterSpec(initialVectorString.getBytes());
//        final Cipher cipher = Cipher.getInstance("AES/CFB8/NoPadding");
//        cipher.init(mode, skeySpec, initialVector);
//        return cipher;
//    }
//
//    private static String md5(final String input) throws NoSuchAlgorithmException {
//        final MessageDigest md = MessageDigest.getInstance("MD5");
//        final byte[] messageDigest = md.digest(input.getBytes());
//        final BigInteger number = new BigInteger(1, messageDigest);
//        return String.format("%032x", number);
//    }
//
//    public String decrypt(final String encryptedData, final String initialVector, final String secretKey) {
//        String decryptedData = null;
//        try {
//            // Initialize the cipher
//            final Cipher cipher = initCipher(Cipher.DECRYPT_MODE, initialVector, secretKey);
//            // Decode using Base64
//            final byte[] encryptedByteArray = (new BASE64Decoder()).decodeBuffer(encryptedData);
//            // Decrypt the data
//            final byte[] decryptedByteArray = cipher.doFinal(encryptedByteArray);
//            decryptedData = new String(decryptedByteArray, "UTF8");
//        } catch (Exception e) {
//            System.err.println("Problem decrypting the data");
//            e.printStackTrace();
//        }
//        return decryptedData;
//    }
//
//    public static String obfuscate(String s) {
//        char[] result = new char[s.length()];
//        for (int i = 0; i < s.length(); i++) {
//            result[i] = (char) (s.charAt(i) + iv.charAt(i % iv.length()));
//        }
//
//        return new String(result);
//    }
//
//    public static String unobfuscate(String s) {
//        char[] result = new char[s.length()];
//        for (int i = 0; i < s.length(); i++) {
//            result[i] = (char) (s.charAt(i) - iv.charAt(i % iv.length()));
//        }
//        return new String(result);
//    }
//}
