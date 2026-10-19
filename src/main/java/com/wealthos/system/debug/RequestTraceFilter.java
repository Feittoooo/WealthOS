package com.wealthos.system.debug;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

/**
 * Gives every API call a request id (the frontend sends one in X-Request-Id, otherwise we make
 * one up), puts it in the logging MDC and keeps the last calls for the debug page. The same id
 * shows up in the browser's network log, the server log and error responses.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestTraceFilter extends OncePerRequestFilter {

    public static final String MDC_KEY = "requestId";
    public static final String HEADER = "X-Request-Id";
    private static final int CAPACITY = 300;
    private static final Logger log = LoggerFactory.getLogger(RequestTraceFilter.class);

    public record Trace(String id, Instant time, String method, String path, int status, long durationMs,
                        String language) {
    }

    private final Deque<Trace> traces = new ArrayDeque<>(CAPACITY);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String id = request.getHeader(HEADER);
        if (id == null || id.isBlank() || id.length() > 64) id = UUID.randomUUID().toString().substring(0, 8);
        MDC.put(MDC_KEY, id);
        response.setHeader(HEADER, id);
        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long ms = (System.nanoTime() - start) / 1_000_000;
            String path = request.getQueryString() == null
                    ? request.getRequestURI()
                    : request.getRequestURI() + "?" + request.getQueryString();
            // the debug page polls itself, keep that out of the log
            if (!request.getRequestURI().startsWith("/api/debug/")) {
                log.info("{} {} -> {} ({} ms)", request.getMethod(), path, response.getStatus(), ms);
                remember(new Trace(id, Instant.now(), request.getMethod(), path, response.getStatus(), ms,
                        request.getHeader("Accept-Language")));
            }
            MDC.remove(MDC_KEY);
        }
    }

    public synchronized List<Trace> recent() {
        List<Trace> list = new ArrayList<>(traces);
        Collections.reverse(list);
        return list;
    }

    public synchronized void clear() {
        traces.clear();
    }

    private synchronized void remember(Trace trace) {
        if (traces.size() == CAPACITY) traces.removeFirst();
        traces.addLast(trace);
    }
}
