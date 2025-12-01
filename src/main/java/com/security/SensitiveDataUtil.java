package com.security;
import com.config.SYSENV;
import org.springframework.beans.factory.annotation.Autowired;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

public final class SensitiveDataUtil {
    private static final SecureRandom RNG = new SecureRandom();
    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;     // 16 bytes tag
    private static final int GCM_IV_BYTES = 12;      // 12 bytes IV recommended for GCM

    private SensitiveDataUtil() {}

    /* ======================
       Masking for DISPLAY
       ====================== */

    /** Mask PAN: keep first 6 and last 4, mask the middle. */
    public static String maskCardPan(String pan) {
        if (pan == null) return null;
        String digits = pan.replaceAll("\\s|-", "");
        if (digits.length() <= 10) {
            // too short—fallback: reveal first 2 and last 2
            int n = Math.max(0, digits.length() - 4);
            return digits.substring(0, Math.min(2, digits.length()))
                    + repeat('*',n)
                    + digits.substring(Math.max(digits.length() - 2, 2));
        }
        String head = digits.substring(0, 6);
        String tail = digits.substring(digits.length() - 4);
        return head + repeat('*',digits.length() - 10) + tail;
    }

    /** Mask account: keep last 4, mask the rest. */
    public static String maskAccount(String account) {
        if (account == null) return null;
        String trimmed = account.replaceAll("\\s|-", "");
        if (trimmed.length() <= 4) return repeat('*',trimmed.length());
        return repeat('*',trimmed.length() - 4) + trimmed.substring(trimmed.length() - 4);
    }

    /** Mask PIN completely for any UI/logging use. */
    public static String maskPin(String pin) {
        if (pin == null) return null;
        return repeat('*',pin.length());
    }

    /* ======================
       Encryption / Decryption
       ====================== */

    /**
     * Encrypt plaintext with AES-GCM.
     * Output format (Base64): IV(12 bytes) || CIPHERTEXT+TAG
     */
    public static String encrypt(String plaintext, String encryptionKey) {
        SecretKey key = aesKeyFromBase64(encryptionKey);
        Objects.requireNonNull(key, "AES key required");
        if (plaintext == null) return null;

        byte[] iv = new byte[GCM_IV_BYTES];
        RNG.nextBytes(iv);

        try {
            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            ByteBuffer bb = ByteBuffer.allocate(iv.length + ct.length);
            bb.put(iv).put(ct);
            return Base64.getEncoder().encodeToString(bb.array());
        } catch (Exception e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    /**
     * Decrypt Base64(IV||CIPHERTEXT+TAG) produced by encrypt().
     */
    public static String decrypt(String base64, String encryptionKey) {
        SecretKey key = aesKeyFromBase64(encryptionKey);
        Objects.requireNonNull(key, "AES key required");
        if (base64 == null) return null;
        byte[] all = Base64.getDecoder().decode(base64);
        if (all.length < GCM_IV_BYTES + 16) { // IV + min tag
            throw new IllegalArgumentException("Invalid cipher payload");
        }
        byte[] iv = new byte[GCM_IV_BYTES];
        byte[] ct = new byte[all.length - GCM_IV_BYTES];
        System.arraycopy(all, 0, iv, 0, GCM_IV_BYTES);
        System.arraycopy(all, GCM_IV_BYTES, ct, 0, ct.length);
        try {
            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] pt = cipher.doFinal(ct);
            return new String(pt, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Decryption failed", e);
        }
    }


    /** Generate a random 256-bit AES key (for bootstrapping/tests). */
    public static SecretKey generateAes256Key() {
        try {
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(256);
            return kg.generateKey();
        } catch (Exception e) {
            throw new IllegalStateException("AES key generation failed", e);
        }
    }

    public static SecretKey aesKeyFromBase64(String base64Key) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            return new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid AES key material", e);
        }
    }
    static String repeat(char ch, int count) {
        if (count <= 0) return "";
        char[] arr = new char[count];
        Arrays.fill(arr, ch);
        return new String(arr);
    }
}
