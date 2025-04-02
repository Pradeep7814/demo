package com.example.demo.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimitingFilter implements Filter {

    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private final Map<String, ClientRequestInfo> clientRequestMap = new ConcurrentHashMap<>();

    @Override
    public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpRequest) {
            String clientIp = httpRequest.getRemoteAddr();
            ClientRequestInfo requestInfo = clientRequestMap.computeIfAbsent(clientIp, k -> new ClientRequestInfo());

            synchronized (requestInfo) {
                long currentTime = Instant.now().getEpochSecond();
                if (currentTime - requestInfo.timestamp > 60) {
                    requestInfo.timestamp = currentTime;
                    requestInfo.requestCount.set(0);
                }

                if (requestInfo.requestCount.incrementAndGet() > MAX_REQUESTS_PER_MINUTE) {
                    ((HttpServletResponse) response).setStatus(429); // 429 is the HTTP status code for Too Many Requests
                    response.getWriter().write("Rate limit exceeded. Try again later.");
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }

    private static class ClientRequestInfo {
        long timestamp = Instant.now().getEpochSecond();
        AtomicInteger requestCount = new AtomicInteger(0);
    }
}
