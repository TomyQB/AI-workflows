package com.example.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * Interceptor that limits concurrency to at most 1 simultaneous request
 * per combination of traceId + endpoint (HTTP method + URI).
 *
 * If a duplicate concurrent request arrives, it is rejected immediately
 * with HTTP 429 (Too Many Requests).
 */
@Component
public class RequestLockInterceptor implements HandlerInterceptor {

    private static final String TRACE_HEADER = "X-Openpay-Trace-Id";
    private static final String LOCK_KEY_ATTRIBUTE = "requestLockKey";

    private final ConcurrentHashMap<String, Semaphore> lockMap = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String traceId = request.getHeader(TRACE_HEADER);
        if (traceId == null || traceId.isBlank()) {
            return true;
        }

        String endpoint = request.getMethod() + ":" + request.getRequestURI();
        String lockKey = traceId + "|" + endpoint;

        Semaphore semaphore = lockMap.computeIfAbsent(lockKey, key -> new Semaphore(1));

        if (!semaphore.tryAcquire()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Concurrent request in progress for traceId: " + traceId);
            return false;
        }

        request.setAttribute(LOCK_KEY_ATTRIBUTE, lockKey);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String lockKey = (String) request.getAttribute(LOCK_KEY_ATTRIBUTE);
        if (lockKey != null) {
            Semaphore semaphore = lockMap.get(lockKey);
            if (semaphore != null) {
                semaphore.release();
                lockMap.remove(lockKey, semaphore);
            }
        }
    }
}
