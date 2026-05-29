package com.internance.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Binds the {@code jwt.*} properties from application configuration.
 *
 * @param secret    HS256 signing key (at least 32 bytes)
 * @param whitelist request paths (Ant patterns) that bypass authentication
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secret, List<String> whitelist) {
}
