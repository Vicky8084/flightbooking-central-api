package central_api.central_api.controller;

import central_api.central_api.service.PaymentService;
import com.razorpay.RazorpayException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    /**
     * Create a new Razorpay order
     * POST /api/payments/create-order
     */
    @PostMapping("/create-order")
    public ResponseEntity<Map<String, String>> createOrder(@RequestBody Map<String, Integer> request) {
        Map<String, String> response = new HashMap<>();

        try {
            int amount = request.get("amount");

            // ✅ Pass a reference ID (can use timestamp or booking ID)
            String orderId = paymentService.createOrder(amount, "R" + System.currentTimeMillis());

            response.put("success", "true");
            response.put("orderId", orderId);
            response.put("amount", String.valueOf(amount));

            System.out.println("✅ Order created: " + orderId + " for amount: ₹" + amount);

        } catch (RazorpayException e) {
            System.err.println("❌ Failed to create order: " + e.getMessage());
            response.put("success", "false");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Verify payment after successful transaction
     * POST /api/payments/verify
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, String>> verifyPayment(@RequestBody Map<String, String> request) {
        Map<String, String> response = new HashMap<>();

        String orderId = request.get("orderId");
        String paymentId = request.get("paymentId");
        String signature = request.get("signature");

        if (orderId == null || paymentId == null || signature == null) {
            response.put("success", "false");
            response.put("message", "Missing required parameters");
            return ResponseEntity.badRequest().body(response);
        }

        boolean isValid = paymentService.verifyPaymentSignature(orderId, paymentId, signature);

        if (isValid) {
            response.put("success", "true");
            response.put("message", "Payment verified successfully");
            System.out.println("✅ Payment verified: " + paymentId);
        } else {
            response.put("success", "false");
            response.put("message", "Payment verification failed");
            System.err.println("❌ Payment verification failed for: " + paymentId);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Get payment status
     * GET /api/payments/status/{paymentId}
     */
    @GetMapping("/status/{paymentId}")
    public ResponseEntity<Map<String, String>> getPaymentStatus(@PathVariable String paymentId) {
        Map<String, String> response = new HashMap<>();

        try {
            String status = paymentService.getPaymentStatus(paymentId);
            response.put("success", "true");
            response.put("status", status);
        } catch (RazorpayException e) {
            response.put("success", "false");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }
}