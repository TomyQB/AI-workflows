package com.openpay.onboarding.config;

import com.openpay.onboarding.interceptor.RequestLockInterceptor;
import com.openpay.onboarding.interceptor.TraceIdInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final TraceIdInterceptor traceIdInterceptor;
    private final RequestLockInterceptor requestLockInterceptor;

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(traceIdInterceptor)
            .addPathPatterns("/api/**");

        registry.addInterceptor(requestLockInterceptor)
            .addPathPatterns("/api/**");
    }
}
