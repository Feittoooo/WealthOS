package com.wealthos.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

// HTTP Basic for one user. Off unless WEALTHOS_PASSWORD is set: you don't want a login on
// localhost, but you do want one on anything public like Railway.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BasicAuthFilter extends OncePerRequestFilter {

    private final byte[] expected;

    public BasicAuthFilter(@Value("${wealthos.auth.username:admin}") String username,
                           @Value("${wealthos.auth.password:}") String password) {
        this.expected = password == null || password.isEmpty()
                ? null
                : (username + ":" + password).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return expected == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.regionMatches(true, 0, "Basic ", 0, 6)) {
            try {
                byte[] given = Base64.getDecoder().decode(header.substring(6).trim());
                if (MessageDigest.isEqual(given, expected)) {
                    chain.doFilter(request, response);
                    return;
                }
            } catch (IllegalArgumentException ignored) {
                // malformed header: fall through to 401
            }
        }
        response.setHeader("WWW-Authenticate", "Basic realm=\"WealthOS\", charset=\"UTF-8\"");
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
