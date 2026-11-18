package com.chinahitech.shop.config;

import com.chinahitech.shop.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        // SSE完成时会发生异步分派，仍需从同一请求凭据恢复身份。
        return false;
    }

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

    /**
     * 从请求头中解析 Bearer Token。
     */
    private String resolveToken(HttpServletRequest request) {
        return JwtUtils.resolveToken(request.getHeader(TOKEN_HEADER), request.getHeader(AUTHORIZATION_HEADER));
    }

    /**
     * 根据用户角色生成 Spring Security 权限列表。
     */
    private List<SimpleGrantedAuthority> authorities(String role) {
        if (!StringUtils.hasText(role)) {
            return Collections.emptyList();
        }
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
