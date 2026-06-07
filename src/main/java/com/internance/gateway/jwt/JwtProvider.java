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

    private final JwtProperties properties;

    public JwtProvider(JwtProperties properties) {
        this.properties = properties;
    }

    /**
     * Validates the token and returns its claims.
     *
     * <p>The signing key is derived from the current {@link JwtProperties} on each call, so
     * a secret rotated via a config refresh takes effect immediately.
     *
     * @throws io.jsonwebtoken.JwtException if validation fails (bad signature, expired, malformed, ...)
     */
    public Claims parseClaims(String token) {
        SecretKey secretKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
