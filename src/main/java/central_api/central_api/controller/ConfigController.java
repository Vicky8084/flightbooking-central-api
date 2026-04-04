package central_api.central_api.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @GetMapping("/razorpay-key")
    public Map<String, String> getRazorpayKey() {
        Map<String, String> response = new HashMap<>();
        response.put("keyId", razorpayKeyId);
        return response;
    }
}