package com.pms.hotel.shared.config;

import com.pms.hotel.shared.web.PublicRateLimitFilter;
import com.pms.hotel.shared.web.RateLimiter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class PublicRateLimitFilterConfig {

    @Bean
    public FilterRegistrationBean<PublicRateLimitFilter> publicRateLimitFilter(RateLimiter rateLimiter) {
        FilterRegistrationBean<PublicRateLimitFilter> registration = new FilterRegistrationBean<>(new PublicRateLimitFilter(rateLimiter));
        registration.addUrlPatterns("/api/v1/public/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
