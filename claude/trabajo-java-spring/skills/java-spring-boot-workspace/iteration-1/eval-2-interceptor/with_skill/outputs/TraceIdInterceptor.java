package com.openpay.onboarding.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
@Slf4j
public class TraceIdInterceptor implements HandlerInterceptor {

    private static final String TRACE_HEADER = "X-Openpay-Trace-Id";
    private static final String MDC_TRACE_KEY = "traceId";

    @Override
    public boolean preHandle(final HttpServletRequest request,
                             final HttpServletResponse response,
                             final Object handler) {
        final var traceId = request.getHeader(TRACE_HEADER);
        if (StringUtils.isNotBlank(traceId)) {
            MDC.put(MDC_TRACE_KEY, traceId);
        } else {
            MDC.put(MDC_TRACE_KEY, UUID.randomUUID().toString());
        }
        return true;
    }

    @Override
    public void afterCompletion(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final Object handler,
                                final Exception ex) {
        MDC.remove(MDC_TRACE_KEY);
    }
}
