package central_api.central_api.service;

import central_api.central_api.client.DbApiClient;
import central_api.central_api.client.NotificationApiClient;
import central_api.central_api.dto.request.LoginRequest;
import central_api.central_api.dto.request.RegisterAirlineRequest;
import central_api.central_api.dto.request.RegisterRequest;
import central_api.central_api.dto.response.AuthResponse;
import central_api.central_api.dto.response.UserResponse;
import central_api.central_api.exception.CustomExceptions;
import central_api.central_api.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final DbApiClient dbApiClient;
    private final NotificationApiClient notificationApiClient;  // ✅ ADD THIS
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthResponse register(RegisterRequest request) {
        System.out.println("🔵 REGISTER CALLED with email: " + request.getEmail());

        // Validate email format
        if (!isValidEmail(request.getEmail())) {
            throw new CustomExceptions.ValidationException("Invalid email format");
        }

        // Check if user already exists
        try {
            UserResponse existingUser = dbApiClient.getUserByEmail(request.getEmail());
            if (existingUser != null) {
                throw new CustomExceptions.ValidationException("Email already registered");
            }
        } catch (Exception e) {
            System.out.println("User not found, proceeding with registration");
        }

        // Create user in DB API
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("email", request.getEmail());
        userMap.put("password", passwordEncoder.encode(request.getPassword()));
        userMap.put("fullName", request.getFullName());
        userMap.put("phoneNumber", request.getPhoneNumber());
        userMap.put("role", request.getRole());

        System.out.println("Sending to DB API: " + userMap);

        Map<String, Object> response = dbApiClient.createUser(userMap);
        System.out.println("DB API Response: " + response);

        // ✅ Safely convert userId from Integer to Long
        Object userIdObj = response.get("userId");
        Long userId;

        if (userIdObj instanceof Integer) {
            userId = ((Integer) userIdObj).longValue();
        } else if (userIdObj instanceof Long) {
            userId = (Long) userIdObj;
        } else if (userIdObj instanceof Number) {
            userId = ((Number) userIdObj).longValue();
        } else {
            userId = null;
        }

        // ✅ SEND WELCOME EMAIL via Notification API
        try {
            Map<String, Object> emailResponse = notificationApiClient.sendWelcomeEmail(
                    request.getEmail(),
                    request.getFullName()
            );
            System.out.println("✅ Welcome email sent: " + emailResponse);
        } catch (Exception e) {
            System.out.println("❌ Failed to send welcome email: " + e.getMessage());
            // Don't fail registration if email fails
        }

        // Generate JWT token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", response.get("role"));
        claims.put("status", response.get("status"));

        String token = jwtUtil.generateToken(request.getEmail(), claims);

        return AuthResponse.builder()
                .token(token)
                .id(userId)
                .email(request.getEmail())
                .fullName(request.getFullName())
                .role(request.getRole())
                .status((String) response.get("status"))
                .build();
    }

    public AuthResponse registerAirline(RegisterAirlineRequest request) {
        // 1. Create airline in DB API
        Map<String, Object> airlineMap = new HashMap<>();
        airlineMap.put("name", request.getAirline().getName());
        airlineMap.put("code", request.getAirline().getCode());
        airlineMap.put("registrationNumber", request.getAirline().getRegistrationNumber());
        airlineMap.put("contactEmail", request.getAirline().getContactEmail());
        airlineMap.put("contactPhone", request.getAirline().getContactPhone());
        airlineMap.put("address", request.getAirline().getAddress());
        airlineMap.put("website", request.getAirline().getWebsite());

        Map<String, Object> airlineResponse = dbApiClient.createAirline(airlineMap);

        // Safely convert airlineId
        Object airlineIdObj = airlineResponse.get("airlineId");
        Long airlineId = convertToLong(airlineIdObj);

        // 2. Create admin user linked to airline
        Map<String, Object> adminMap = new HashMap<>();
        adminMap.put("email", request.getAdmin().getEmail());
        adminMap.put("password", passwordEncoder.encode(request.getAdmin().getPassword()));
        adminMap.put("fullName", request.getAdmin().getFullName());
        adminMap.put("phoneNumber", request.getAdmin().getPhone());
        adminMap.put("role", "AIRLINE_ADMIN");

        Map<String, Object> airline = new HashMap<>();
        airline.put("id", airlineId);
        adminMap.put("airline", airline);

        Map<String, Object> adminResponse = dbApiClient.createUser(adminMap);
        Long userId = convertToLong(adminResponse.get("userId"));

        // ✅ 3. Prepare data for emails
        Map<String, Object> airlineData = new HashMap<>();
        airlineData.put("airlineName", request.getAirline().getName());
        airlineData.put("airlineCode", request.getAirline().getCode());
        airlineData.put("registrationNumber", request.getAirline().getRegistrationNumber());
        airlineData.put("contactEmail", request.getAirline().getContactEmail());
        airlineData.put("contactPhone", request.getAirline().getContactPhone());
        airlineData.put("address", request.getAirline().getAddress());
        airlineData.put("website", request.getAirline().getWebsite());
        airlineData.put("adminFullName", request.getAdmin().getFullName());
        airlineData.put("adminEmail", request.getAdmin().getEmail());
        airlineData.put("adminPhone", request.getAdmin().getPhone());
        airlineData.put("designation", request.getAdmin().getDesignation());
        airlineData.put("department", request.getAdmin().getDepartment());

        // ✅ 4. Send welcome email to AIRLINE_ADMIN (pending status)
        try {
            notificationApiClient.sendAirlineWelcomeEmail(request.getAdmin().getEmail(), airlineData);
            System.out.println("✅ Welcome email sent to airline admin: " + request.getAdmin().getEmail());
        } catch (Exception e) {
            System.out.println("❌ Failed to send welcome email: " + e.getMessage());
        }

        // ✅ 5. Send pending notification to SYSTEM_ADMIN
        try {
            Map<String, Object> pendingData = new HashMap<>(airlineData);
            // Add URLs for approval/rejection
            pendingData.put("approveUrl", "http://localhost:8081/api/admin/airlines/" + airlineId + "/approve?adminId=1");
            pendingData.put("rejectUrl", "http://localhost:8081/api/admin/airlines/" + airlineId + "/reject?adminId=1");

            notificationApiClient.sendAirlinePendingNotification(
                    "airnova.helpteam@gmail.com",  // SYSTEM_ADMIN email
                    pendingData
            );
            System.out.println("✅ Pending notification sent to SYSTEM_ADMIN");
        } catch (Exception e) {
            System.out.println("❌ Failed to send pending notification: " + e.getMessage());
        }

        return AuthResponse.builder()
                .id(userId)
                .email(request.getAdmin().getEmail())
                .fullName(request.getAdmin().getFullName())
                .role("AIRLINE_ADMIN")
                .airlineId(airlineId)
                .status("PENDING")
                .message("Registration successful! Awaiting admin approval. Check your email for confirmation.")
                .build();
    }

    // Helper method for type conversion
    private Long convertToLong(Object obj) {
        if (obj instanceof Integer) {
            return ((Integer) obj).longValue();
        } else if (obj instanceof Long) {
            return (Long) obj;
        } else if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        return null;
    }

    public AuthResponse login(LoginRequest request) {
        try {
            UserResponse user = dbApiClient.getUserByEmail(request.getEmail());

            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", user.getId());
            claims.put("role", user.getRole());
            claims.put("airlineId", user.getAirlineId());
            claims.put("status", user.getStatus());

            String token = jwtUtil.generateToken(user.getEmail(), claims);

            return AuthResponse.builder()
                    .token(token)
                    .id(user.getId())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .role(user.getRole())
                    .airlineId(user.getAirlineId())
                    .status(user.getStatus())
                    .build();

        } catch (Exception e) {
            throw new CustomExceptions.InvalidCredentialsException("Invalid email or password");
        }
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email.matches(emailRegex);
    }

    // Add this method if not present
    public void forgotPassword(String email) {
        try {
            // Check if user exists in DB API
            UserResponse user = dbApiClient.getUserByEmail(email);

            if (user == null) {
                // Don't reveal that user doesn't exist
                System.out.println("Forgot password request for non-existent email: " + email);
                return;
            }

            // Call Notification API to send OTP
            Map<String, Object> otpRequest = new HashMap<>();
            otpRequest.put("email", email);
            otpRequest.put("name", user.getFullName());
            otpRequest.put("type", "PASSWORD_RESET");

            notificationApiClient.generateOtp(otpRequest);

            System.out.println("OTP sent to: " + email);

        } catch (Exception e) {
            // Don't reveal if user exists or not
            System.out.println("Forgot password error: " + e.getMessage());
        }
    }

    public void resetPassword(String email, String newPassword) {
        try {
            // Get user by email
            UserResponse user = dbApiClient.getUserByEmail(email);

            if (user == null) {
                throw new CustomExceptions.UserNotFoundException("User not found");
            }

            // Encode new password
            String encodedPassword = passwordEncoder.encode(newPassword);

            // Update password in DB API
            Map<String, Object> updateRequest = new HashMap<>();
            updateRequest.put("password", encodedPassword);

            // Call DB API to update password
            dbApiClient.updatePassword(user.getId(), encodedPassword);

            System.out.println("Password reset successful for: " + email);

        } catch (Exception e) {
            throw new CustomExceptions.BadRequestException("Unable to reset password");
        }
    }
}