package com.chinahitech.shop.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordUtilTest {

    @Test
    void encodeCreatesBCryptHashThatMatchesRawPassword() {
        String encodedPassword = PasswordUtil.encode("secret123");

        assertNotEquals("secret123", encodedPassword);
        assertTrue(PasswordUtil.matches(encodedPassword, "secret123"));
        assertFalse(PasswordUtil.matches(encodedPassword, "wrong"));
    }
}
