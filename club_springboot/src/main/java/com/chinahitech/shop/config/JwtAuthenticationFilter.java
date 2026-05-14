package com.chinahitech.shop.config;

import com.chinahitech.shop.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String TOKEN_HEADER = "X-Token";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (StringUtils.hasText(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(request, token);
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, String token) {
        try {
            Claims claims = JwtUtils.getClaimsByToken(token);
            String userId = claims.getSubject();
            if (!StringUtils.hasText(userId)) {
                return;
            }
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    authorities(claims.get("role", String.class))
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception ignored) {
            SecurityContextHolder.clearContext();
        }
    }

    private String resolveToken(HttpServletRequest request) {
        return JwtUtils.resolveToken(request.getHeader(TOKEN_HEADER), request.getHeader(AUTHORIZATION_HEADER));
    }

    private List<SimpleGrantedAuthority> authorities(String role) {
        if (!StringUtils.hasText(role)) {
            return Collections.emptyList();
        }
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
