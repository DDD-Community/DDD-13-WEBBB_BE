package com.ddd.webbb.global.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(MdcFilter.class);
    private static final String REQUEST_ID_KEY = "requestId";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9\\-]{1,64}$");
    private static final Set<String> SENSITIVE_PARAMS =
            Set.of("token", "access_token", "refresh_token", "password", "email", "secret", "key");

    private static final Set<String> EXCLUDED_PREFIXES =
            Set.of("/swagger-ui", "/v3/api-docs", "/actuator");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null
                || requestId.isBlank()
                || !REQUEST_ID_PATTERN.matcher(requestId).matches()) {
            requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        }

        MDC.put(REQUEST_ID_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        long start = System.currentTimeMillis();
        log.info(">>> {} {} {}", request.getMethod(), request.getRequestURI(), maskQuery(request));

        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            log.info("<<< {} {}ms", response.getStatus(), elapsed);
            MDC.clear();
        }
    }

    private String maskQuery(HttpServletRequest request) {
        String query = request.getQueryString();
        if (query == null || query.isBlank()) {
            return "";
        }

        StringBuilder masked = new StringBuilder("?");
        String[] pairs = query.split("&");
        for (int i = 0; i < pairs.length; i++) {
            if (i > 0) {
                masked.append('&');
            }
            int eq = pairs[i].indexOf('=');
            if (eq < 0) {
                masked.append(pairs[i]);
                continue;
            }
            String key = pairs[i].substring(0, eq).toLowerCase();
            if (SENSITIVE_PARAMS.contains(key)) {
                masked.append(pairs[i], 0, eq).append("=***");
            } else {
                masked.append(pairs[i]);
            }
        }
        return masked.toString();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return EXCLUDED_PREFIXES.stream().anyMatch(path::startsWith);
    }
}
