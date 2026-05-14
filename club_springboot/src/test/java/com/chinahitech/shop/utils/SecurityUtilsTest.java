package com.chinahitech.shop.utils;

import com.chinahitech.shop.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("SecurityUtils unit tests")
class SecurityUtilsTest {

    @Test
    @DisplayName("requireSelf - same user returns current user id")
    void testRequireSelf_SameUser_ReturnUserId() {
        // Given - an authenticated user and matching requested id
        Authentication authentication = new UsernamePasswordAuthenticationToken("1001", null);

        // When - checking self access
        String result = SecurityUtils.requireSelf(authentication, "1001");

        // Then - the current user id is returned
        assertEquals("1001", result);
    }

    @Test
    @DisplayName("requireSelf - missing authentication throws exception")
    void testRequireSelf_NullAuthentication_ThrowException() {
        // When & Then - unauthenticated access is rejected
        assertThrows(BusinessException.class, () -> SecurityUtils.requireSelf(null, "1001"));
    }

    @Test
    @DisplayName("requireSelf - blank requested id throws exception")
    void testRequireSelf_BlankRequestedUserId_ThrowException() {
        // Given - an authenticated user
        Authentication authentication = new UsernamePasswordAuthenticationToken("1001", null);

        // When & Then - blank requested id is rejected
        assertThrows(BusinessException.class, () -> SecurityUtils.requireSelf(authentication, " "));
    }

    @Test
    @DisplayName("requireSelf - different user throws exception")
    void testRequireSelf_DifferentUser_ThrowException() {
        // Given - an authenticated user and another requested id
        Authentication authentication = new UsernamePasswordAuthenticationToken("1001", null);

        // When & Then - cross-user access is rejected
        assertThrows(BusinessException.class, () -> SecurityUtils.requireSelf(authentication, "2002"));
    }
}
