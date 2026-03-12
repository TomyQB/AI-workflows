package com.openpay.onboarding.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;

@Component
@Slf4j
public class RequestLockInterceptor implements HandlerInterceptor {

    private final ConcurrentMap<String, Semaphore> endpointSemaphores = new ConcurrentHashMap<>();
    private static final int MAX_CONCURRENT_REQUESTS = 1;

    @Override
    public boolean preHandle(final HttpServletRequest request,
                             final HttpServletResponse response,
                             final Object handler) throws IOException {
        final var traceId = request.getHeader("X-Openpay-Trace-Id");
        final var lockKey = "%s_%s".formatted(request.getRequestURI(), traceId);

        final var semaphore = endpointSemaphores
            .computeIfAbsent(lockKey, key -> new Semaphore(MAX_CONCURRENT_REQUESTS));

        if (!semaphore.tryAcquire()) {
            log.warn("Concurrent request blocked: {} traceId={}", request.getRequestURI(), traceId);
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(),
                "Concurrent request in progress");
            return false;
        }

        request.setAttribute("lockKey", lockKey);
        return true;
    }

    @Override
    public void afterCompletion(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final Object handler,
                                final Exception ex) {
        final var lockKey = (String) request.getAttribute("lockKey");
        if (StringUtils.isNotBlank(lockKey)) {
            final var semaphore = endpointSemaphores.get(lockKey);
            if (Objects.nonNull(semaphore)) {
                semaphore.release();
            }
        }
    }
}
