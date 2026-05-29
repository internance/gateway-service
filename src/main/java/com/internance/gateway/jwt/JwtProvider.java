package com.internance.gateway.jwt;

import com.internance.gateway.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Verifies the JWT signature and parses its claims.
 * Throws an {@link io.jsonwebtoken.JwtException} for expired or tampered tokens.
 */
@Component
public class JwtProvider {

    private final SecretKey secretKey;

    public JwtProvider(JwtProperties properties) {
        this.secretKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Validates the token and returns its claims.
     *
     * @throws io.jsonwebtoken.JwtException if validation fails (bad signature, expired, malformed, ...)
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
