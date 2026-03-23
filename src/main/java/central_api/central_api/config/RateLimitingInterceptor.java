package central_api.central_api.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimitingInterceptor implements HandlerInterceptor {

    private final ConcurrentHashMap<String, RateLimitingConfig.RateLimitInfo> rateLimits;
    private final int maxRequestsPerMinute;
    private final long resetIntervalMs;

    public RateLimitingInterceptor(ConcurrentHashMap<String, RateLimitingConfig.RateLimitInfo> rateLimits,
                                   int maxRequestsPerMinute, long resetIntervalMs) {
        this.rateLimits = rateLimits;
        this.maxRequestsPerMinute = maxRequestsPerMinute;
        this.resetIntervalMs = resetIntervalMs;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = getClientIp(request);
        String key = clientIp + ":" + request.getRequestURI();

        RateLimitingConfig.RateLimitInfo info = rateLimits.computeIfAbsent(key, k -> new RateLimitingConfig.RateLimitInfo());

        long now = System.currentTimeMillis();
        if (now > info.getResetTime()) {
            info.getRequestCount().set(0);
            info.setResetTime(now + resetIntervalMs);
        }

        int count = info.getRequestCount().incrementAndGet();

        if (count > maxRequestsPerMinute) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Too many requests. Please try again later.\"}");
            return false;
        }

        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}