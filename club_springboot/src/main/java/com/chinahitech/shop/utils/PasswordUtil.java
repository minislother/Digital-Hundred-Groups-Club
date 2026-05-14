package com.chinahitech.shop.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Password hashing utility based on BCrypt.
 */
public class PasswordUtil {

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    /**
     * Hash a raw password before storing it.
     */
    public static String encode(String password) {
        return PASSWORD_ENCODER.encode(password);
    }

    /**
     * Compare a stored hash with the raw password supplied during login.
     */
    public static boolean matches(String encodedPassword, String rawPassword) {
        return PASSWORD_ENCODER.matches(rawPassword, encodedPassword);
    }
}
