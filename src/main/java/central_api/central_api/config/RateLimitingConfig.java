package central_api.central_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class RateLimitingConfig implements WebMvcConfigurer {

    private final ConcurrentHashMap<String, RateLimitInfo> rateLimits = new ConcurrentHashMap<>();

    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private static final long RESET_INTERVAL_MS = 60000; // 1 minute

    @Bean
    public RateLimitingInterceptor rateLimitingInterceptor() {
        return new RateLimitingInterceptor(rateLimits, MAX_REQUESTS_PER_MINUTE, RESET_INTERVAL_MS);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitingInterceptor())
                .addPathPatterns("/api/auth/**", "/api/notify/otp/**");
    }

    public static class RateLimitInfo {
        private final AtomicInteger requestCount;
        private long resetTime;

        public RateLimitInfo() {
            this.requestCount = new AtomicInteger(0);
            this.resetTime = System.currentTimeMillis() + RESET_INTERVAL_MS;
        }

        public AtomicInteger getRequestCount() {
            return requestCount;
        }

        public long getResetTime() {
            return resetTime;
        }

        public void setResetTime(long resetTime) {
            this.resetTime = resetTime;
        }
    }
}