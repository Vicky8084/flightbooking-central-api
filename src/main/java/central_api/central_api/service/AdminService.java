package central_api.central_api.service;

import central_api.central_api.client.DbApiClient;
import central_api.central_api.client.NotificationApiClient;
import central_api.central_api.exception.CustomExceptions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final DbApiClient dbApiClient;
    private final NotificationApiClient notificationApiClient;

    public List<Map<String, Object>> getPendingAirlines() {
        return dbApiClient.getPendingAirlines();
    }

    public List<Map<String, Object>> getActiveAirlines() {
        return dbApiClient.getActiveAirlines();
    }

    /**
     * ✅ Approve Airline - Updates both airline and user status to ACTIVE
     */
    public Map<String, Object> approveAirline(Long airlineId, Long adminId) {
        try {
            // First, get airline details before approval
            Map<String, Object> airlineDetails = dbApiClient.getAirlineById(airlineId);

            if (airlineDetails == null) {
                throw new CustomExceptions.AirlineNotFoundException("Airline not found with ID: " + airlineId);
            }

            String currentStatus = (String) airlineDetails.get("status");
            if ("ACTIVE".equals(currentStatus)) {
                throw new CustomExceptions.BadRequestException("Airline is already active");
            }

            // Approve in DB (this should update both airline and user status)
            Map<String, Object> response = dbApiClient.approveAirline(airlineId, adminId);

            // Send approval email to airline admin
            try {
                String adminEmail = (String) airlineDetails.get("contactEmail");
                String airlineName = (String) airlineDetails.get("name");
                String airlineCode = (String) airlineDetails.get("code");
                String registrationNumber = (String) airlineDetails.get("registrationNumber");

                Map<String, Object> emailData = new HashMap<>();
                emailData.put("airlineName", airlineName);
                emailData.put("airlineCode", airlineCode);
                emailData.put("registrationNumber", registrationNumber);
                emailData.put("dashboardUrl", "http://localhost:8081/airline-dashboard");

                notificationApiClient.sendAirlineApproved(adminEmail, emailData);
                System.out.println("✅ Approval email sent to: " + adminEmail);
            } catch (Exception e) {
                System.out.println("❌ Failed to send approval email: " + e.getMessage());
            }

            return response;
        } catch (CustomExceptions.AirlineNotFoundException e) {
            throw e;
        } catch (CustomExceptions.BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomExceptions.AirlineNotFoundException("Airline not found with ID: " + airlineId);
        }
    }

    /**
     * ✅ Reject Airline - Updates both airline and user status to REJECTED
     */
    public Map<String, Object> rejectAirline(Long airlineId, Long adminId, String reason) {
        try {
            // First, get airline details before rejection
            Map<String, Object> airlineDetails = dbApiClient.getAirlineById(airlineId);

            if (airlineDetails == null) {
                throw new CustomExceptions.AirlineNotFoundException("Airline not found with ID: " + airlineId);
            }

            String currentStatus = (String) airlineDetails.get("status");
            if ("REJECTED".equals(currentStatus)) {
                throw new CustomExceptions.BadRequestException("Airline is already rejected");
            }

            // Reject in DB (this should update both airline and user status)
            Map<String, Object> response = dbApiClient.rejectAirline(airlineId, adminId, reason);

            // Send rejection email to airline admin
            try {
                String adminEmail = (String) airlineDetails.get("contactEmail");
                String airlineName = (String) airlineDetails.get("name");
                String airlineCode = (String) airlineDetails.get("code");
                String registrationNumber = (String) airlineDetails.get("registrationNumber");

                Map<String, Object> emailData = new HashMap<>();
                emailData.put("airlineName", airlineName);
                emailData.put("airlineCode", airlineCode);
                emailData.put("registrationNumber", registrationNumber);
                emailData.put("rejectionReason", reason != null ? reason : "No specific reason provided.");
                emailData.put("registerUrl", "http://localhost:8081/signup");

                notificationApiClient.sendAirlineRejected(adminEmail, emailData);
                System.out.println("✅ Rejection email sent to: " + adminEmail);
            } catch (Exception e) {
                System.out.println("❌ Failed to send rejection email: " + e.getMessage());
            }

            return response;
        } catch (CustomExceptions.AirlineNotFoundException e) {
            throw e;
        } catch (CustomExceptions.BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomExceptions.AirlineNotFoundException("Airline not found with ID: " + airlineId);
        }
    }
}