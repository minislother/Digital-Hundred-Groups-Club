package com.chinahitech.shop.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;

public class JwtUtils {
    //7天过期
    private static final long expire = 604800;
    private static final String BEARER_PREFIX = "Bearer ";
    public static final String ROLE_STUDENT = "STUDENT";
    public static final String ROLE_MANAGER = "MANAGER";
    public static final String ROLE_TOP_MANAGER = "TOP_MANAGER";

    //生成token
    public static String generateToken(String username){
        return generateToken(username, null);
    }

    public static String generateToken(String username, String role){
        Date now = new Date();
        Date expiration = new Date(now.getTime() + 1000 * expire);
        io.jsonwebtoken.JwtBuilder builder = Jwts.builder()
                .setHeaderParam("type","JWT")
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiration);
        if (role != null && !role.trim().isEmpty()) {
            builder.claim("role", role);
        }
        return builder.signWith(SignatureAlgorithm.HS512, getSecret())
                .compact();
    }

    //解析token
    public static Claims getClaimsByToken(String token){
        return Jwts.parser()
                    .setSigningKey(getSecret())
                    .parseClaimsJws(token)
                    .getBody();

    }

    public static String resolveToken(String xToken, String authorization) {
        if (xToken != null && !xToken.trim().isEmpty()) {
            return xToken;
        }
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            return authorization.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private static String getSecret() {
        String secret = System.getProperty("jwt.secret");
        if (secret == null || secret.trim().isEmpty()) {
            secret = System.getenv("JWT_SECRET");
        }
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("JWT_SECRET is not configured");
        }
        return secret;
    }
}
