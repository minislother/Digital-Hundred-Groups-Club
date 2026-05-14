package com.chinahitech.shop.utils;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("JwtUtils unit tests")
class JwtUtilsTest {

    @BeforeEach
    void setUp() {
        System.setProperty("jwt.secret", "test-jwt-secret-test-jwt-secret-test-jwt-secret");
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("jwt.secret");
    }

    @Test
    @DisplayName("generateToken - role claim is written and parsed")
    void testGenerateToken_WithRole_ParseClaims() {
        // Given - a user id and role
        String userId = "1001";

        // When - generating and parsing a token
        String token = JwtUtils.generateToken(userId, JwtUtils.ROLE_MANAGER);
        Claims claims = JwtUtils.getClaimsByToken(token);

        // Then - subject and role are preserved
        assertEquals(userId, claims.getSubject());
        assertEquals(JwtUtils.ROLE_MANAGER, claims.get("role", String.class));
    }

    @Test
    @DisplayName("generateToken - no role keeps role claim empty")
    void testGenerateToken_NoRole_RoleClaimMissing() {
        // When - generating a compatibility token without role
        String token = JwtUtils.generateToken("1001");
        Claims claims = JwtUtils.getClaimsByToken(token);

        // Then - the token is still valid but has no role claim
        assertEquals("1001", claims.getSubject());
        assertNull(claims.get("role", String.class));
    }

    @Test
    @DisplayName("resolveToken - X-Token has priority over Authorization")
    void testResolveToken_XTokenPreferred_ReturnXToken() {
        // When - both token headers are present
        String token = JwtUtils.resolveToken("x-token", "Bearer bearer-token");

        // Then - X-Token is preferred
        assertEquals("x-token", token);
    }

    @Test
    @DisplayName("resolveToken - bearer token is extracted")
    void testResolveToken_BearerHeader_ReturnBearerToken() {
        // When - only Authorization bearer token is present
        String token = JwtUtils.resolveToken(null, "Bearer bearer-token");

        // Then - bearer prefix is removed
        assertEquals("bearer-token", token);
    }

    @Test
    @DisplayName("resolveToken - missing headers returns null")
    void testResolveToken_MissingHeaders_ReturnNull() {
        // When - no token headers are present
        String token = JwtUtils.resolveToken(null, null);

        // Then - no token can be resolved
        assertNull(token);
    }
}
