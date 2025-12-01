/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.helper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.cert.CertificateException;
import java.security.cert.Certificate;

import com.DTO.KYC.zcsra.DemographicDataRequestPayload;
import com.DTO.ubx.RequestWrapper;
import com.config.SYSENV;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import me.sniggle.pgp.crypt.PGPMessageEncryptor;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

/**
 *
 * @author samichael
 */
public class EncryptUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptUtil.class);
    private static final SYSENV env = new SYSENV();
    private static final int IV_SIZE = 12; // 12 bytes
    private static final int SALT_SIZE = 16; // 16 bytes
    private static final int KEY_LENGTH = 256; // 256 bits
    private static final int ITERATIONS = 65536; // PBKDF2 iterations
    private static final int GCM_TAG_LENGTH = 128; // GCM Tag length

    public static String decryptPGPfile(String content, String keyPath, String password) {
        String rawFile = "-1";
        LOGGER.info("decryptPGPfile:input-> {}", content);
        try {
            PGPMessageEncryptor encryptor = new PGPMessageEncryptor();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            encryptor.decrypt(password, new FileInputStream(new File(keyPath)), new FileInputStream(new File(content)), baos);
            rawFile = baos.toString();
            baos.close();
        } catch (FileNotFoundException ex) {
            LOGGER.error("decryptPGPfile:FileNotFoundException:error", ex);
        } catch (IOException ex) {
            LOGGER.error("decryptPGPfile:IOException:error", ex);
        }
        LOGGER.info("decryptPGPfile:output-> {}", rawFile);

        return rawFile;

    }

    public static String decryptPGPStr(String content, String keyPath, String password) {
        String rawFile = "-1";
        LOGGER.info("decryptPGPfile:input-> {}", content);
        try {
            PGPMessageEncryptor encryptor = new PGPMessageEncryptor();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            encryptor.decrypt(password, new FileInputStream(new File(keyPath)), new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), baos);
            rawFile = baos.toString();
            baos.close();
        } catch (FileNotFoundException ex) {
            LOGGER.error("decryptPGPfile:FileNotFoundException:error", ex);
        } catch (IOException ex) {
            LOGGER.error("decryptPGPfile:IOException:error", ex);
        }
        LOGGER.info("decryptPGPfile:output-> {}", rawFile);

        return rawFile;

    }

    public static String encryptPGPfile(String content, String publickeyPath, String password) throws InvalidKeySpecException, NoSuchAlgorithmException {
        String rawFile = "-1";
        LOGGER.info("decryptPGPfile:input-> {}", content);
        try {
            File publicKeyFile = new File("public.key");
            byte[] publicKeyBytes = Files.readAllBytes(publicKeyFile.toPath());

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
            keyFactory.generatePublic(publicKeySpec);
            PGPMessageEncryptor encryptor = new PGPMessageEncryptor();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            encryptor.encrypt(new FileInputStream(new File(publickeyPath)), password, new FileInputStream(new File(content)), baos);
            rawFile = baos.toString();
            baos.close();
        } catch (FileNotFoundException ex) {
            LOGGER.error("decryptPGPfile:FileNotFoundException:error", ex);
        } catch (IOException ex) {
            LOGGER.error("decryptPGPfile:IOException:error", ex);
        }
        LOGGER.info("decryptPGPfile:output-> {}", rawFile);

        return rawFile;

    }

    public static String encryptPGPStr(String content, String publickeyPath, String password) {
        String rawFile = "-1";
        LOGGER.info("decryptPGPfile:input-> {}", content);
        try {
            PGPMessageEncryptor encryptor = new PGPMessageEncryptor();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            encryptor.encrypt(new FileInputStream(new File(publickeyPath)), password, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), baos);
            rawFile = baos.toString();
            baos.close();
        } catch (FileNotFoundException ex) {
            LOGGER.error("decryptPGPfile:FileNotFoundException:error", ex);
        } catch (IOException ex) {
            LOGGER.error("decryptPGPfile:IOException:error", ex);
        }
        LOGGER.info("decryptPGPfile:output-> {}", rawFile);

        return rawFile;

    }

    public static String encryptRSAStr(String content, String keyPath) {
        String rawFile = "-1";
        LOGGER.info("encryptRASStr:input-> {}", content);
        try {
            File publicKeyFile = new File(keyPath);
            byte[] publicKeyBytes = Files.readAllBytes(publicKeyFile.toPath());
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
            Cipher encryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] secretMessageBytes = content.getBytes(StandardCharsets.UTF_8);
            byte[] encryptedMessageBytes = encryptCipher.doFinal(secretMessageBytes);
            String encodedMessage = Base64.getEncoder().encodeToString(encryptedMessageBytes);
            rawFile = encodedMessage;
        } catch (FileNotFoundException | NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
            LOGGER.error("encryptRASStr:FileNotFoundException:error", ex);
        } catch (IOException ex) {
            LOGGER.error("encryptRASStr:IOException:error", ex);
        }
        LOGGER.info("encryptRASStr:output-> {}", rawFile);

        return rawFile;

    }

    public static String decryptRSAStr(String content, String keyPath) {
        String rawFile = "-1";
        LOGGER.info("decryptRASfile:input-> {}", content);
        try {
            byte[] keyBytes = Files.readAllBytes(Paths.get(keyPath));
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = kf.generatePrivate(spec);
            Cipher decryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] encryptedMessageBytes = Base64.getDecoder().decode(content);;
            byte[] decryptedMessageBytes = decryptCipher.doFinal(encryptedMessageBytes);
            String decryptedMessage = new String(decryptedMessageBytes, StandardCharsets.UTF_8);
            rawFile = decryptedMessage;
        } catch (FileNotFoundException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | InvalidKeySpecException | IllegalBlockSizeException | BadPaddingException ex) {
            LOGGER.error("decryptRASfile:error", ex);
        } catch (IOException ex) {
            LOGGER.error("decryptRASfile:IOException:error", ex);
        }
        LOGGER.info("decryptRASfile:output-> {}", rawFile);

        return rawFile;

    }

    public static SecretKey getKeyFromPassword(String password, String salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), 65536, 256);
        SecretKey secret = new SecretKeySpec(factory.generateSecret(spec)
                .getEncoded(), "AES");
        return secret;
    }
    public static SecretKey generateAESKey(String password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = keyFactory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }
    public static byte[] generateIV() {
        byte[] iv = new byte[IV_SIZE];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
    public static byte[] generateSalt() {
        byte[] salt = new byte[SALT_SIZE];
        new SecureRandom().nextBytes(salt);
        return salt;
    }
    public static String encryptPayload(String data, String password) throws Exception {
        // Generate IV and Salt with random data
        byte[] iv = generateIV(); // Generate random IV
        byte[] salt = generateSalt(); // Generate random Salt

        // Derive AES Key
        SecretKey key = generateAESKey(password, salt);

        // Encrypt Data
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);
        byte[] encryptedData = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

        // Validate encrypted data is not null or empty
        if (encryptedData == null || encryptedData.length == 0) {
            throw new IllegalStateException("Encryption produced invalid data.");
        }

        // Combine IV, Salt, and Encrypted Data
        int payloadLength = IV_SIZE + SALT_SIZE + encryptedData.length;
        byte[] finalPayload = new byte[payloadLength];
        System.arraycopy(iv, 0, finalPayload, 0, IV_SIZE); // Copy IV
        System.arraycopy(salt, 0, finalPayload, IV_SIZE, SALT_SIZE); // Copy Salt
        System.arraycopy(encryptedData, 0, finalPayload, IV_SIZE + SALT_SIZE, encryptedData.length); // Copy Encrypted Data

        // Encode Final Payload to Base64
        return Base64.getEncoder().encodeToString(finalPayload);
    }
    public static String createSignature(String data, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(data.getBytes(StandardCharsets.UTF_8));
        byte[] signatureBytes = signature.sign();
        return Base64.getEncoder().encodeToString(signatureBytes);
    }
    public static RequestWrapper encryptAndSignPayload(String payload,String pass, PrivateKey privateKey) throws Exception {
        String encryptedPayload = encryptPayload(payload,pass);
        String signature = createSignature(payload, privateKey);
        return new RequestWrapper(encryptedPayload, signature);
    }
    public static String decryptResponse(String encryptedPayload, String password) throws Exception {
        byte[] encryptedDataWithIvAndSalt = Base64.getDecoder().decode(encryptedPayload);
        // Extract IV, Salt, and Encrypted Data
        byte[] iv = generateIV();
        System.arraycopy(encryptedDataWithIvAndSalt, 0, iv, 0, IV_SIZE);

        byte[] salt = generateSalt();
        System.arraycopy(encryptedDataWithIvAndSalt, IV_SIZE, salt, 0, SALT_SIZE);

        int encryptedDataLength = encryptedDataWithIvAndSalt.length - IV_SIZE - SALT_SIZE;
        byte[] encryptedData = new byte[encryptedDataLength];
        System.arraycopy(encryptedDataWithIvAndSalt, IV_SIZE + SALT_SIZE, encryptedData, 0, encryptedDataLength);

        // Derive Key
        SecretKey key = EncryptUtil.generateAESKey(password, salt);

        // Decrypt Payload
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);
        byte[] decryptedBytes = cipher.doFinal(encryptedData);

        // Return decrypted data as string
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
    public static boolean isSignatureVerified(String data, String signature, PublicKey publicKey) throws Exception {
        LOGGER.info("Data Signed: {}", data);
        LOGGER.info("Signature: {}", signature);
        byte[] signatureBytes = Base64.getDecoder().decode(signature);
        LOGGER.info("Decoded Signature Length: {}", signatureBytes.length);
        Signature signatureInstance = Signature.getInstance("SHA256withRSA");
        signatureInstance.initVerify(publicKey);
        signatureInstance.update(data.getBytes(StandardCharsets.UTF_8));
        boolean isVerified = signatureInstance.verify(signatureBytes);
        LOGGER.info("Signature Verified: {}", isVerified);
        return isVerified;
    }

//    public static String encryptAESStr(String input) throws NoSuchPaddingException, NoSuchAlgorithmException,
//            InvalidAlgorithmParameterException, InvalidKeyException,
//            BadPaddingException, IllegalBlockSizeException, UnsupportedEncodingException {
//
//        GCMParameterSpec iv = new GCMParameterSpec(128, "12".getBytes());
//
//        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
//        cipher.init(Cipher.ENCRYPT_MODE,generateAESKey(input), iv);
//        byte[] cipherText = cipher.doFinal(input.getBytes());
//        return new String(cipherText);
//    }

    public static String decryptAESStr(byte[] cipherText, SecretKey key, String ivSize12) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException, UnsupportedEncodingException {
        GCMParameterSpec iv = new GCMParameterSpec(128, ivSize12.getBytes());

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        byte[] plainText = cipher.doFinal(cipherText);
        return new String(plainText);
    }

    public static void chunkDecrypt(byte[] ciphertext, SecretKeySpec key, byte[] iv) throws Exception {

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));

        int chunkSize = 16;
        byte[] inBuffer = new byte[chunkSize];
        int outBufferSize = ((chunkSize + 15) / 16) * 16;
        byte[] outBuffer = new byte[outBufferSize];

        for (int i = 0; i < ciphertext.length; i += chunkSize) {
            int thisChunkSize = Math.min(chunkSize, ciphertext.length - i);
            System.arraycopy(ciphertext, i, inBuffer, 0, thisChunkSize);
            int num = cipher.update(inBuffer, 0, thisChunkSize, outBuffer);
            if (num > 0) {
                System.out.println("update #" + ((i / chunkSize) + 1) + " - data <"
                        + new String(outBuffer, 0, num) + ">");
            }
        }
        int num = cipher.doFinal(inBuffer, chunkSize, 0, outBuffer);
        System.out.println("doFinal - data <" + new String(outBuffer, 0, num) + ">");
    }

    public static String signZCSRAPayload(String payload, PrivateKey privateKey) {
        LOGGER.info("Payload to signe in json String {}", payload);
        try {
            Signature signature = Signature.getInstance("SHA1withRSA");
            signature.initSign(privateKey);
            byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
            signature.update(payloadBytes);
            byte[] signed = signature.sign();
            return Base64.getEncoder().encodeToString(signed);

        }catch (Exception e) {
            LOGGER.error("Failed to generate signature", e);
        }
        return null;

    }
    public static PrivateKey loadPrivateKey(String privateKeyPath,String alias, String password) {
        try {
            ClassPathResource resource = new ClassPathResource(privateKeyPath);
            InputStream inputStream = resource.getInputStream();
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(inputStream, password.toCharArray());
            Key key = keyStore.getKey(alias, password.toCharArray());
            if (key instanceof PrivateKey) {
                return (PrivateKey) key;
            } else {
                throw new KeyStoreException("Key is not a private key");
            }
        } catch (KeyStoreException e) {
            System.err.println("KeyStoreException: " + e.getMessage());
        } catch (CertificateException e) {
            System.err.println("CertificateException: " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            System.err.println("NoSuchAlgorithmException: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Exception: " + e.getMessage());
        }
        return null;
    }
    public static boolean verifyZCSRASignature(String payload, String signatureBase64, PublicKey publicKey) {
        try {
            Signature signature = Signature.getInstance("SHA1withRSA");
            if (publicKey == null) {
                LOGGER.error("Failed to generate public key");
                return false;
            }
            signature.initVerify(publicKey);
            byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
            signature.update(payloadBytes);
            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);

            boolean isVerified = signature.verify(signatureBytes);
            if (isVerified) {
                LOGGER.info("Signature verified successfully.");
            } else {
                LOGGER.error("Signature verification failed.");
            }
            return isVerified;
        } catch (Exception e) {
            LOGGER.error("Failed to verify signature", e);
        }
        return false;
    }
    public static PublicKey loadPublicKey(String keystorePath, String alias, String keystorePassword)  {
        ClassPathResource resource = new ClassPathResource(keystorePath);
        try (InputStream keyStoreStream = resource.getInputStream()) {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(keyStoreStream, keystorePassword.toCharArray());
            Certificate cert = keyStore.getCertificate(alias);
            PublicKey pk = cert.getPublicKey();
            LOGGER.info("Public key loaded successfully: {}",pk);
            return pk;
        }catch (Exception e) {
            LOGGER.error("Failed to load public key", e);
        }

        return null;
    }

    public static String getAllTestedHere(String payload,String pvtkPath,String pvtkAlias, String pvtkPass,  String pkPath, String pkAlias, String pkPass) {
        PublicKey publicKey = loadPublicKey(pkPath,pkAlias,pkPass);
        PrivateKey privateKey = loadPrivateKey(pvtkPath,pvtkAlias,pvtkPass);
        String signature = signZCSRAPayload(payload,privateKey);
        boolean verify = verifyZCSRASignature(payload,signature,publicKey);
        if (verify) {
            return signature;
        }else {
            LOGGER.error("Failed to verify signature");
            return null;
        }

    }

}
