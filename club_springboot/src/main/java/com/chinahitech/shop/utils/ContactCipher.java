package com.chinahitech.shop.utils;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.function.Function;

/** 带版本、字段认证的联系方式加密。检索密钥与加密密钥分离。 */
public final class ContactCipher {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final ContactCipher DEFAULT = new ContactCipher(System::getenv);
    private final Function<String, String> config;
    ContactCipher(Function<String, String> config) { this.config = config; }
    private String setting(String name, String fallback) {
        String value = config.apply(name);
        return value == null || value.isBlank() ? fallback : value;
    }
    private byte[] key(String name) {
        String value = config.apply(name);
        if (value == null) throw new IllegalStateException(name + " is required");
        byte[] key = Base64.getDecoder().decode(value);
        if (key.length != 32) throw new IllegalStateException(name + " requires 32 bytes");
        return key;
    }
    private String active() {
        String id = setting("CONTACT_ACTIVE_KEY_ID", "primary");
        if (!id.matches("[A-Za-z0-9_]{1,32}")) throw new IllegalStateException("Invalid contact key id");
        return id;
    }
    private byte[] versionKey(String id) {
        if (!id.matches("[A-Za-z0-9_]{1,32}")) throw new IllegalStateException("Invalid contact key id");
        return key("CONTACT_KEY_" + id);
    }
    public static String encrypt(String value) { return encrypt(value, "contact"); }
    public static String decrypt(String value) { return decrypt(value, "contact"); }
    public static String encrypt(String value, String field) { return DEFAULT.seal(value, field); }
    public static String decrypt(String value, String field) { return DEFAULT.open(value, field, false); }
    public static String lookup(String value, String field) { return DEFAULT.digest(value, field); }
    public static String verificationKey(String email) { return "email-code:" + lookup(email, "email"); }
    String seal(String value, String field) {
        if (value == null || value.isEmpty()) return value;
        try {
            String id = active();
            byte[] nonce = new byte[12]; RANDOM.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(versionKey(id), "AES"), new GCMParameterSpec(128, nonce));
            cipher.updateAAD(("enc:v2:" + id + ":user." + field).getBytes(StandardCharsets.UTF_8));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[12 + encrypted.length];
            System.arraycopy(nonce, 0, payload, 0, 12);
            System.arraycopy(encrypted, 0, payload, 12, encrypted.length);
            return "enc:v2:" + id + ":" + Base64.getEncoder().encodeToString(payload);
        } catch (Exception e) { throw new IllegalStateException("Contact encryption failed", e); }
    }
    String open(String value, String field, boolean migration) {
        if (value == null || value.isEmpty()) return value;
        try {
            if (!value.startsWith("enc:")) {
                if (!migration && Boolean.parseBoolean(setting("CONTACT_STRICT_READ", "true")))
                    throw new IllegalStateException("Plaintext contact rejected");
                return value;
            }
            String payloadText; byte[] secret; String aad = null;
            if (value.startsWith("enc:v1:")) {
                if (!migration && Boolean.parseBoolean(setting("CONTACT_STRICT_READ", "true")))
                    throw new IllegalStateException("Legacy contact requires migration");
                payloadText = value.substring(7); secret = key("CONTACT_ENCRYPTION_KEY");
            } else {
                String[] parts = value.split(":", 4);
                if (parts.length != 4 || !parts[1].equals("v2")) throw new IllegalArgumentException("Invalid contact format");
                secret = versionKey(parts[2]); payloadText = parts[3];
                aad = "enc:v2:" + parts[2] + ":user." + field;
            }
            byte[] payload = Base64.getDecoder().decode(payloadText);
            if (payload.length < 28) throw new IllegalArgumentException("Truncated contact");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(secret, "AES"), new GCMParameterSpec(128, payload, 0, 12));
            if (aad != null) cipher.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
            return new String(cipher.doFinal(payload, 12, payload.length - 12), StandardCharsets.UTF_8);
        } catch (Exception e) { throw new IllegalStateException("Contact decryption failed", e); }
    }
    String digest(String value, String field) {
        if (value == null || value.isBlank()) return null;
        try {
            // 邮箱仅规范化域名，保留本地部分；手机号不擅自合并国家码。
            String normalized = value.trim();
            if (field.equals("email")) {
                int at = normalized.lastIndexOf('@');
                if (at >= 0) normalized = normalized.substring(0, at) + normalized.substring(at).toLowerCase(Locale.ROOT);
            }
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key("CONTACT_LOOKUP_KEY"), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(("user." + field + ":" + normalized).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new IllegalStateException("Contact lookup failed", e); }
    }
    static ContactCipher migrationCipher() { return DEFAULT; }
}
