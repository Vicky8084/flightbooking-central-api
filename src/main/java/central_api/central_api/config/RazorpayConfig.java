package central_api.central_api.config;

import com.razorpay.RazorpayClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RazorpayConfig {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Bean
    public RazorpayClient razorpayClient() throws Exception {
        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            System.out.println("✅ Razorpay Client initialized successfully in TEST MODE");
            return client;
        } catch (Exception e) {
            System.err.println("❌ Failed to initialize Razorpay Client: " + e.getMessage());
            throw e;
        }
    }
}