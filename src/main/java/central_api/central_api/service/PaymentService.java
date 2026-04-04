package central_api.central_api.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentService {

    @Autowired
    private RazorpayClient razorpayClient;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    /**
     * Create a Razorpay Order
     * @param amount Amount in rupees (will be converted to paise)
     * @param receiptId Unique receipt ID
     * @return Order ID from Razorpay
     */
    public String createOrder(int amount, String receiptId) throws RazorpayException {
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amount * 100); // Convert to paise
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", receiptId);
        orderRequest.put("payment_capture", 1); // Auto capture payment

        Order order = razorpayClient.orders.create(orderRequest);
        return order.get("id");
    }

    /**
     * Verify Payment Signature (Server-side verification)
     * @param orderId Razorpay Order ID
     * @param paymentId Razorpay Payment ID
     * @param signature Razorpay Signature
     * @return true if signature is valid
     */
    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        try {
            JSONObject options = new JSONObject();
            // ✅ FIXED: Razorpay expects these exact field names
            options.put("razorpay_order_id", orderId);
            options.put("razorpay_payment_id", paymentId);
            options.put("razorpay_signature", signature);

            boolean isValid = Utils.verifyPaymentSignature(options, keySecret);

            if (isValid) {
                System.out.println("✅ Payment signature verified successfully for Order: " + orderId);
            } else {
                System.err.println("❌ Payment signature verification FAILED for Order: " + orderId);
            }

            return isValid;
        } catch (RazorpayException e) {
            System.err.println("❌ Signature verification error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get payment status from Razorpay
     * @param paymentId Razorpay Payment ID
     * @return Payment status (captured, authorized, failed)
     */
    public String getPaymentStatus(String paymentId) throws RazorpayException {
        com.razorpay.Payment payment = razorpayClient.payments.fetch(paymentId);
        return payment.get("status");
    }

    /**
     * Capture a payment (if not auto-captured)
     * @param paymentId Razorpay Payment ID
     * @param amount Amount to capture (in paise)
     */
    public void capturePayment(String paymentId, int amount) throws RazorpayException {
        JSONObject captureRequest = new JSONObject();
        captureRequest.put("amount", amount);
        razorpayClient.payments.capture(paymentId, captureRequest);
    }

    /**
     * Refund a payment
     * @param paymentId Razorpay Payment ID
     * @param amount Amount to refund (in paise, null for full refund)
     */
    public void refundPayment(String paymentId, Integer amount) throws RazorpayException {
        JSONObject refundRequest = new JSONObject();
        if (amount != null) {
            refundRequest.put("amount", amount);
        }
        razorpayClient.payments.refund(paymentId, refundRequest);
    }
}