/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.helper;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
//import java.security.interfaces.RSAPrivateKey;
//import java.security.spec.InvalidKeySpecException;
//import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import com.queue.QueueProducer;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.pkcs.RSAPrivateKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author melleji.mollel
 */
@Component
public class SignRequest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SignRequest.class);

    private PrivateKey getPrivateKey(String keyPass, String keyAlias, String keyFilePath) throws Exception {
//        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        KeyStore keyStore = KeyStore.getInstance("JKS");
        FileInputStream is = new FileInputStream(keyFilePath);

        keyStore.load(is, keyPass.toCharArray());
        return (PrivateKey) keyStore.getKey(keyAlias, keyPass.toCharArray());
    }

    public String CreateSignature(String content, String privateKeyPass, String privateKeyAlias, String privateKeyFilePath) throws Exception {
        byte[] data = content.getBytes(StandardCharsets.UTF_8);
        Signature sig = Signature.getInstance("SHA256WithRSA");
        PrivateKey privateKey = getPrivateKey(privateKeyPass, privateKeyAlias, privateKeyFilePath);
        sig.initSign(privateKey);
        sig.update(data);
        byte[] signatureBytes = sig.sign();
        return Base64.encodeBase64String(signatureBytes);
    }



    private PublicKey getPublicKey(String keyPass, String keyAlias, String keyFilePath) throws Exception {
//        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        KeyStore keyStore = KeyStore.getInstance("JKS");
        FileInputStream is = new FileInputStream(keyFilePath);
        keyStore.load(is, keyPass.toCharArray());
        Certificate cert = keyStore.getCertificate(keyAlias);
        return cert.getPublicKey();

    }

    public boolean verifySignature(String signature, String content, String publicKeyPass, String publicKeyAlias,
            String publicKeyFilePath) throws Exception {
        boolean t = false;
        try {
            byte[] db = org.apache.commons.codec.binary.Base64.decodeBase64(signature.getBytes());
            Signature sig = Signature.getInstance("SHA256withRSA");
            byte[] data = content.getBytes(StandardCharsets.UTF_8);
            sig.initVerify(getPublicKey(publicKeyPass, publicKeyAlias, publicKeyFilePath));
            sig.update(data);
            t = sig.verify(db);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return t;
    }



//    todo signature with PEM

    public  PrivateKey loadPrivateKeyPem(String pemFilePath) throws Exception {
        FileInputStream fis = new FileInputStream(pemFilePath);
        byte[] keyBytes = new byte[fis.available()];
        fis.read(keyBytes);
        fis.close();

        // Remove the header, footer, and newlines from the PEM file
        String privateKeyPEM = new String(keyBytes)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replaceAll(System.lineSeparator(), "")
                .replace("-----END PRIVATE KEY-----", "");

        // Decode base64-encoded private key
        byte[] decodedKey = Base64.decodeBase64(privateKeyPEM);
//                getDecoder().decode(privateKeyPEM);

        // Create a PKCS8EncodedKeySpec from the decoded key bytes
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedKey);

        // Generate private key
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    public  String createSignatureWithPem(String content, String pemFilePath) throws Exception {
        byte[] data = content.getBytes();
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(loadPrivateKeyPem(pemFilePath));
        sig.update(data);
        byte[] signatureBytes = sig.sign();
        return Base64.encodeBase64String(signatureBytes);
    }

    public static String generateSignature(String data, String keyPass, String keyAlias, String keyFilePath) throws Exception {
        try {
            String signedHashString = "";

            // Load the keystore
            KeyStore keystore = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream(keyFilePath)) {
                keystore.load(fis, keyPass.toCharArray());
            }
            PrivateKey privateKey = (PrivateKey) keystore.getKey(keyAlias, keyPass.toCharArray());

            if (privateKey != null) {
                // Compute SHA-1 hash
                MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
                byte[] hash = sha1.digest(data.getBytes(StandardCharsets.UTF_8));

                // Sign the hash
                Signature signature = Signature.getInstance("SHA1withRSA");
                signature.initSign(privateKey);
                signature.update(hash);
                byte[] signedHash = signature.sign();

                // Convert to Base64
                signedHashString = Base64.encodeBase64String(signedHash);
            } else {
                LOGGER.info("No private key");
            }

            return signedHashString;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String generateSignature256(String data, String keyPass, String keyAlias, String keyFilePath) throws Exception {
        String signedHashString = "";

        // Load the keystore
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        FileInputStream is = new FileInputStream(keyFilePath);

        keyStore.load(is, keyPass.toCharArray());
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(keyAlias, keyPass.toCharArray());

        if (privateKey != null) {
            Signature sig = Signature.getInstance("SHA256WithRSA");
            sig.initSign(privateKey);
            sig.update(data.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = sig.sign();

            // Convert to Base64
            signedHashString = Base64.encodeBase64String(signatureBytes);
        }

        return signedHashString;
    }

//    public  PublicKey loadPublicKey(String pemFilePath) throws Exception {
//        FileInputStream fis = new FileInputStream(pemFilePath);
//        byte[] keyBytes = new byte[fis.available()];
//        fis.read(keyBytes);
//        fis.close();
//
//        // Remove the header, footer, and newlines from the PEM file
////        String publicKeyPEM =StringUtils.substringBetween(Arrays.toString(keyBytes), "-----BEGIN PUBLIC KEY-----", "-----END PUBLIC KEY-----").replaceAll("\\n", "");
////        LOGGER.info("publicKeyPEM ..... \n{} " ,publicKeyPEM);
//        String publicKeyPEM = new String(keyBytes)
//                .replace("-----BEGIN PRIVATE KEY-----", "")
//                .replaceAll(System.lineSeparator(), "")
//                .replace("-----END CERTIFICATE-----", "");
//
//        // Decode base64-encoded public key
//        byte[] decodedKey = Base64.decodeBase64(publicKeyPEM);
//
//        // Create a X509EncodedKeySpec from the decoded key bytes
//        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedKey);
//
//        // Generate public key
//        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
//        return keyFactory.generatePublic(keySpec);
//    }
//
//
//    public  boolean verifySignatureWithPem(String signature, String content, String publicKeyFilePath) throws Exception {
//        boolean t = false;
//        try {
//            byte[] db = Base64.decodeBase64(signature.getBytes());
//            Signature sig = Signature.getInstance("SHA256withRSA");
//            PublicKey loadedkey = loadPublicKey(publicKeyFilePath);
//            byte[] data = content.getBytes();
//            sig.initVerify(loadedkey);
//            sig.update(data);
//            t = sig.verify(db);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return t;
//    }



}
