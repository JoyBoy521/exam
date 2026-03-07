package com.exam.system.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class RequestTimingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestTimingFilter.class);
    private static final long SLOW_THRESHOLD_MS = 800;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String traceId = UUID.randomUUID().toString().replace("-", "");
        response.setHeader("X-Trace-Id", traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            long cost = System.currentTimeMillis() - start;
            String path = request.getRequestURI();
            int status = response.getStatus();
            if (cost >= SLOW_THRESHOLD_MS) {
                log.warn("REQ traceId={} method={} path={} status={} costMs={}",
                        traceId, request.getMethod(), path, status, cost);
            } else {
                log.info("REQ traceId={} method={} path={} status={} costMs={}",
                        traceId, request.getMethod(), path, status, cost);
            }
        }
    }
}
