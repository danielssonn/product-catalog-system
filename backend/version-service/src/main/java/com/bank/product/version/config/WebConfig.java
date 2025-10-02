package com.bank.product.version.config;

import com.bank.product.version.interceptor.ApiVersionInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web Configuration
 * Registers API version interceptor
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final ApiVersionInterceptor apiVersionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiVersionInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/v1/versions/**", "/actuator/**");
    }
}
