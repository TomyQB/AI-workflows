package com.example.config;

import com.example.interceptor.RequestLockInterceptor;
import com.example.interceptor.TraceIdInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC configuration that registers the trace-id and request-lock interceptors.
 * TraceIdInterceptor runs first (order 0) so the MDC is populated before
 * RequestLockInterceptor (order 1) evaluates concurrency.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final TraceIdInterceptor traceIdInterceptor;
    private final RequestLockInterceptor requestLockInterceptor;

    public WebMvcConfig(TraceIdInterceptor traceIdInterceptor, RequestLockInterceptor requestLockInterceptor) {
        this.traceIdInterceptor = traceIdInterceptor;
        this.requestLockInterceptor = requestLockInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(traceIdInterceptor)
                .addPathPatterns("/**")
                .order(0);

        registry.addInterceptor(requestLockInterceptor)
                .addPathPatterns("/**")
                .order(1);
    }
}
