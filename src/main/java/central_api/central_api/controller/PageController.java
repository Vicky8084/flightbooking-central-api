package central_api.central_api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping({"/", "/home"})
    public String home() {
        return "home";
    }

    @GetMapping("/signin")
    public String signIn() {
        return "signin";
    }

    @GetMapping("/signup")
    public String signUp() {
        return "signup";
    }

    @GetMapping("/flights/search")
    public String searchFlights() {
        return "search-flights";
    }

    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "forgot-password";
    }

    @GetMapping("/system-admin-login")
    public String systemAdminLogin() {
        return "system-admin-login";
    }

    @GetMapping("/system-dashboard")
    public String systemDashboard() {
        return "system-dashboard";
    }

    @GetMapping("/airline-dashboard")
    public String airlineDashboard() {
        return "airline-dashboard";
    }

    // ✅ FIX: User Dashboard Page Mapping
    @GetMapping("/user-dashboard")
    public String userDashboard() {
        return "user-dashboard";
    }

    // ✅ FIX: My Bookings Page
    @GetMapping("/my-bookings")
    public String myBookings() {
        return "user-dashboard";  // Same page, different tab
    }

    // ✅ FIX: Booking Details Page
    @GetMapping("/booking-details")
    public String bookingDetails() {
        return "user-dashboard";  // Opens modal on dashboard
    }
}