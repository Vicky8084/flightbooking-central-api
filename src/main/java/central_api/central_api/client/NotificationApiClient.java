package central_api.central_api.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "notification-api", url = "${notification.api.url:http://localhost:8084}")
public interface NotificationApiClient {

    @PostMapping("/api/notify/email/welcome")
    Map<String, Object> sendWelcomeEmail(@RequestParam("to") String to, @RequestParam("name") String name);

    @PostMapping("/api/notify/email/booking-confirmation")
    Map<String, Object> sendBookingConfirmation(@RequestParam("to") String to, @RequestBody Map<String, Object> bookingData);

    @PostMapping("/api/notify/email/booking-cancellation")
    Map<String, Object> sendBookingCancellation(@RequestParam("to") String to, @RequestBody Map<String, Object> bookingData);

    @PostMapping("/api/notify/otp/generate")
    Map<String, Object> generateOtp(@RequestBody Map<String, Object> otpRequest);

    @PostMapping("/api/notify/email/airline-welcome")
    Map<String, Object> sendAirlineWelcomeEmail(@RequestParam("to") String to, @RequestBody Map<String, Object> airlineData);

    @PostMapping("/api/notify/email/airline-pending")
    Map<String, Object> sendAirlinePendingNotification(@RequestParam("to") String to, @RequestBody Map<String, Object> airlineData);

    @PostMapping("/api/notify/email/airline-approved")
    Map<String, Object> sendAirlineApproved(@RequestParam("to") String to, @RequestBody Map<String, Object> airlineData);

    @PostMapping("/api/notify/email/airline-rejected")
    Map<String, Object> sendAirlineRejected(@RequestParam("to") String to, @RequestBody Map<String, Object> airlineData);

    @PostMapping("/api/notify/email/booking-confirmation-multiple")
    Map<String, Object> sendBookingConfirmationToMultiple(
            @RequestBody Map<String, Object> request);
}