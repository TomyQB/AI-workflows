package com.example.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * Interceptor that reads the X-Openpay-Trace-Id header and propagates it into the MDC
 * for structured logging. If the header is absent, a new UUID is generated as fallback.
 */
@Component
public class TraceIdInterceptor implements HandlerInterceptor {

    private static final String TRACE_HEADER = "X-Openpay-Trace-Id";
    private static final String MDC_TRACE_KEY = "traceId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String traceId = request.getHeader(TRACE_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }
        MDC.put(MDC_TRACE_KEY, traceId);
        response.setHeader(TRACE_HEADER, traceId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        MDC.remove(MDC_TRACE_KEY);
    }
}
