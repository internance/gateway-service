package com.internance.gateway.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Request wrapper that adds/overrides headers derived from the verified JWT claims.
 *
 * <p>Any client-supplied header with the same name (e.g. {@code X-User-Id}) is ignored and
 * replaced by the value injected here before it reaches downstream services. Header names
 * are compared case-insensitively.
 */
public class UserContextRequestWrapper extends HttpServletRequestWrapper {

    private final Map<String, String> overriddenHeaders;

    public UserContextRequestWrapper(HttpServletRequest request, Map<String, String> headers) {
        super(request);
        this.overriddenHeaders = new LinkedHashMap<>();
        headers.forEach((name, value) -> this.overriddenHeaders.put(name.toLowerCase(), value));
    }

    @Override
    public String getHeader(String name) {
        String overridden = overriddenHeaders.get(name.toLowerCase());
        return overridden != null ? overridden : super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        String overridden = overriddenHeaders.get(name.toLowerCase());
        if (overridden != null) {
            return Collections.enumeration(Collections.singletonList(overridden));
        }
        return super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        Set<String> names = new LinkedHashSet<>();
        Enumeration<String> original = super.getHeaderNames();
        while (original.hasMoreElements()) {
            names.add(original.nextElement());
        }
        names.addAll(overriddenHeaders.keySet());
        return Collections.enumeration(names);
    }
}
