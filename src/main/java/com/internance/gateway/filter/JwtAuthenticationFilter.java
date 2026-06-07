package com.internance.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.internance.common.api.ApiResponse;
import com.internance.common.exception.ErrorCode;
import com.internance.common.exception.GlobalErrorCode;
import com.internance.common.filter.UserContextFilter;
import com.internance.gateway.config.JwtProperties;
import com.internance.gateway.jwt.JwtProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Validates the JWT of every request entering the gateway.
 *
 * <ul>
 *   <li>Whitelisted paths (login/sign-up, actuator, ...) pass through without validation.</li>
 *   <li>For a valid token, the authenticated user id is forwarded downstream via the
 *       {@code X-User-Id} header so that the shared {@code UserContextFilter} can pick it up.</li>
 *   <li>A missing, tampered or expired token results in a {@code 401} response using the
 *       shared {@link ApiResponse} format.</li>
 * </ul>
 *
 * This is a servlet (WebMVC) based Spring Cloud Gateway, so it is implemented as an
 * {@link OncePerRequestFilter} rather than a reactive {@code GlobalFilter}.
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final JwtProperties properties;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthenticationFilter(JwtProvider jwtProvider,
                                   JwtProperties properties,
                                   ObjectMapper objectMapper) {
        this.jwtProvider = jwtProvider;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Read the whitelist live so a config refresh (see ConfigChangeListener) takes effect.
        return properties.getWhitelist().stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token == null) {
            sendError(response, GlobalErrorCode.UNAUTHORIZED, "Missing authentication token.");
            return;
        }

        try {
            Claims claims = jwtProvider.parseClaims(token);
            String subject = claims.getSubject();
            if (!StringUtils.hasText(subject)) {
                sendError(response, GlobalErrorCode.UNAUTHORIZED, "Invalid authentication token.");
                return;
            }

            // Override any client-supplied X-User-Id so only the verified subject reaches downstream services.
            UserContextRequestWrapper wrapped = new UserContextRequestWrapper(request, Map.of(
                    UserContextFilter.USER_ID_HEADER, subject
            ));
            filterChain.doFilter(wrapped, response);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            sendError(response, GlobalErrorCode.UNAUTHORIZED, "Invalid authentication token.");
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(bearer) && bearer.startsWith(BEARER_PREFIX)) {
            return bearer.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }

    private void sendError(HttpServletResponse response, ErrorCode errorCode, String message) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(errorCode, message));
    }
}
