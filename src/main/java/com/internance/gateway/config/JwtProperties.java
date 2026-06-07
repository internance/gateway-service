package com.internance.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Binds the {@code jwt.*} properties from application configuration.
 *
 * <p>Declared as a mutable JavaBean (rather than a record) so Spring Cloud's
 * {@code ConfigurationPropertiesRebinder} can re-bind it <em>in place</em> when
 * {@code ConfigChangeListener} triggers a context refresh — record properties cannot be
 * rebound because their fields are final. The JWT beans read it live, so a refreshed
 * secret/whitelist takes effect without a restart.
 */
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** HS256 signing key (at least 32 bytes). */
    private String secret;

    /** Request paths (Ant patterns) that bypass authentication. */
    private List<String> whitelist = List.of();

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public List<String> getWhitelist() {
        return whitelist;
    }

    public void setWhitelist(List<String> whitelist) {
        this.whitelist = (whitelist == null) ? List.of() : List.copyOf(whitelist);
    }
}
