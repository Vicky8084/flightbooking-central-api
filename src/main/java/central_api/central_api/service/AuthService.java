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
    private final NotificationApiClient notificationApiClient;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * ✅ USER REGISTRATION - CUSTOMER gets ACTIVE immediately
     */
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

        // Create user - CUSTOMER gets ACTIVE status immediately
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("email", request.getEmail());
        userMap.put("password", passwordEncoder.encode(request.getPassword()));
        userMap.put("fullName", request.getFullName());
        userMap.put("phoneNumber", request.getPhoneNumber());
        userMap.put("role", request.getRole());
        userMap.put("status", "ACTIVE");  // ✅ Customer is ACTIVE immediately

        System.out.println("Sending to DB API: " + userMap);

        Map<String, Object> response = dbApiClient.createUser(userMap);
        System.out.println("DB API Response: " + response);

        Long userId = convertToLong(response.get("userId"));

        // Send welcome email
        try {
            notificationApiClient.sendWelcomeEmail(request.getEmail(), request.getFullName());
            System.out.println("✅ Welcome email sent to: " + request.getEmail());
        } catch (Exception e) {
            System.out.println("❌ Failed to send welcome email: " + e.getMessage());
        }

        // Generate JWT token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", request.getRole());
        claims.put("status", "ACTIVE");

        String token = jwtUtil.generateToken(request.getEmail(), claims);

        return AuthResponse.builder()
                .token(token)
                .id(userId)
                .email(request.getEmail())
                .fullName(request.getFullName())
                .role(request.getRole())
                .status("ACTIVE")
                .build();
    }

    /**
     * ✅ AIRLINE REGISTRATION - User gets PENDING status until airline is approved
     */
    public AuthResponse registerAirline(RegisterAirlineRequest request) {
        System.out.println("🔵 AIRLINE REGISTER CALLED for: " + request.getAirline().getName());

        // 1. Create airline with PENDING status
        Map<String, Object> airlineMap = new HashMap<>();
        airlineMap.put("name", request.getAirline().getName());
        airlineMap.put("code", request.getAirline().getCode());
        airlineMap.put("registrationNumber", request.getAirline().getRegistrationNumber());
        airlineMap.put("contactEmail", request.getAirline().getContactEmail());
        airlineMap.put("contactPhone", request.getAirline().getContactPhone());
        airlineMap.put("address", request.getAirline().getAddress());
        airlineMap.put("website", request.getAirline().getWebsite());
        airlineMap.put("status", "PENDING");  // ✅ Airline starts as PENDING

        Map<String, Object> airlineResponse = dbApiClient.createAirline(airlineMap);
        Long airlineId = convertToLong(airlineResponse.get("airlineId"));

        // 2. Create admin user with PENDING status (NOT ACTIVE!)
        Map<String, Object> adminMap = new HashMap<>();
        adminMap.put("email", request.getAdmin().getEmail());
        adminMap.put("password", passwordEncoder.encode(request.getAdmin().getPassword()));
        adminMap.put("fullName", request.getAdmin().getFullName());
        adminMap.put("phoneNumber", request.getAdmin().getPhone());
        adminMap.put("role", "AIRLINE_ADMIN");
        adminMap.put("status", "PENDING");  // ✅ ✅ ✅ CRITICAL: User status is PENDING until approval
        adminMap.put("airline", Map.of("id", airlineId));

        Map<String, Object> adminResponse = dbApiClient.createUser(adminMap);
        Long userId = convertToLong(adminResponse.get("userId"));

        // 3. Prepare data for emails
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

        // 4. Send welcome email to airline admin (pending status)
        try {
            notificationApiClient.sendAirlineWelcomeEmail(request.getAdmin().getEmail(), airlineData);
            System.out.println("✅ Welcome email sent to airline admin: " + request.getAdmin().getEmail());
        } catch (Exception e) {
            System.out.println("❌ Failed to send welcome email: " + e.getMessage());
        }

        // 5. Send pending notification to SYSTEM_ADMIN
        try {
            Map<String, Object> pendingData = new HashMap<>(airlineData);
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

        // ❌ NO TOKEN GENERATED FOR PENDING USER
        // User cannot login until approved

        return AuthResponse.builder()
                .id(userId)
                .email(request.getAdmin().getEmail())
                .fullName(request.getAdmin().getFullName())
                .role("AIRLINE_ADMIN")
                .airlineId(airlineId)
                .status("PENDING")
                .message("Registration successful! Awaiting admin approval. You will receive an email when approved.")
                .build();
    }

    /**
     * ✅ LOGIN - Check if user is ACTIVE
     */
    public AuthResponse login(LoginRequest request) {
        try {
            // Get user from DB API
            UserResponse user = dbApiClient.getUserByEmail(request.getEmail());

            if (user == null) {
                throw new CustomExceptions.InvalidCredentialsException("Invalid email or password");
            }

            // ✅ ✅ ✅ CRITICAL FIX: Check if user is ACTIVE
            if (!"ACTIVE".equals(user.getStatus())) {
                String message;
                if ("PENDING".equals(user.getStatus())) {
                    message = "Your account is pending approval. Please wait for admin approval.";
                } else if ("REJECTED".equals(user.getStatus())) {
                    message = "Your account has been rejected. Please contact support.";
                } else if ("SUSPENDED".equals(user.getStatus())) {
                    message = "Your account has been suspended. Please contact support.";
                } else {
                    message = "Account is not active. Status: " + user.getStatus();
                }
                throw new CustomExceptions.UnauthorizedException(message);
            }

            // Generate tokens
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", user.getId());
            claims.put("role", user.getRole());
            claims.put("airlineId", user.getAirlineId());
            claims.put("status", user.getStatus());

            String token = jwtUtil.generateToken(user.getEmail(), claims);
            String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getEmail());

            return AuthResponse.builder()
                    .token(token)
                    .refreshToken(refreshToken)
                    .id(user.getId())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .role(user.getRole())
                    .airlineId(user.getAirlineId())
                    .status(user.getStatus())
                    .message("Login successful")
                    .build();

        } catch (CustomExceptions.UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomExceptions.InvalidCredentialsException("Invalid email or password");
        }
    }

    /**
     * ✅ GET USER FROM TOKEN
     */
    public Map<String, Object> getUserFromToken(String token) {
        if (!jwtUtil.validateToken(token)) {
            throw new CustomExceptions.InvalidCredentialsException("Invalid token");
        }

        Long userId = jwtUtil.extractUserId(token);
        String email = jwtUtil.extractUsername(token);
        String role = jwtUtil.extractRole(token);
        Long airlineId = jwtUtil.extractAirlineId(token);
        String status = jwtUtil.extractStatus(token);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", userId);
        userInfo.put("email", email);
        userInfo.put("role", role);
        userInfo.put("airlineId", airlineId);
        userInfo.put("status", status);

        return userInfo;
    }

    /**
     * ✅ FORGOT PASSWORD
     */
    public void forgotPassword(String email) {
        try {
            UserResponse user = dbApiClient.getUserByEmail(email);

            if (user == null) {
                System.out.println("Forgot password request for non-existent email: " + email);
                return;
            }

            Map<String, Object> otpRequest = new HashMap<>();
            otpRequest.put("email", email);
            otpRequest.put("name", user.getFullName());
            otpRequest.put("type", "PASSWORD_RESET");

            notificationApiClient.generateOtp(otpRequest);
            System.out.println("OTP sent to: " + email);

        } catch (Exception e) {
            System.out.println("Forgot password error: " + e.getMessage());
        }
    }

    /**
     * ✅ RESET PASSWORD
     */
    public void resetPassword(String email, String newPassword) {
        try {
            UserResponse user = dbApiClient.getUserByEmail(email);

            if (user == null) {
                throw new CustomExceptions.UserNotFoundException("User not found");
            }

            String encodedPassword = passwordEncoder.encode(newPassword);
            dbApiClient.updatePassword(user.getId(), encodedPassword);

            System.out.println("Password reset successful for: " + email);

        } catch (Exception e) {
            throw new CustomExceptions.BadRequestException("Unable to reset password");
        }
    }

    // ========== HELPER METHODS ==========

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

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email.matches(emailRegex);
    }


}