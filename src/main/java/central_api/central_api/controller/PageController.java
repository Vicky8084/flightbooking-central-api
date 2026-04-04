package central_api.central_api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

    // ✅ NEW: Search Results Page
    @GetMapping("/flights/results")
    public String searchResults(@RequestParam(required = false) String sourceCode,
                                @RequestParam(required = false) String destinationCode,
                                @RequestParam(required = false) String travelDate,
                                @RequestParam(required = false) String sourceCity,
                                @RequestParam(required = false) String destinationCity,
                                @RequestParam(required = false) Integer passengers,
                                @RequestParam(required = false) String seatClass,
                                @RequestParam(required = false) Boolean includeConnecting,
                                @RequestParam(required = false) Double maxPrice,
                                org.springframework.ui.Model model) {

        model.addAttribute("sourceCode", sourceCode);
        model.addAttribute("destinationCode", destinationCode);
        model.addAttribute("travelDate", travelDate);
        model.addAttribute("sourceCity", sourceCity);
        model.addAttribute("destinationCity", destinationCity);
        model.addAttribute("passengers", passengers != null ? passengers : 1);
        model.addAttribute("seatClass", seatClass != null ? seatClass : "ECONOMY");
        model.addAttribute("includeConnecting", includeConnecting != null ? includeConnecting : true);
        model.addAttribute("maxPrice", maxPrice);

        return "search-results";
    }

    // ✅ ADD THIS - Booking Page (Only for logged in users)
    @GetMapping("/booking")
    public String booking() {
        return "booking";
    }

    @GetMapping("/price-comparison")
    public String priceComparison() {
        return "price-comparison";
    }

    @GetMapping("/passenger-details")
    public String passengerDetails(@RequestParam(required = false) Long flightId,
                                   @RequestParam(required = false) String fareCode,
                                   @RequestParam(required = false) Integer passengers,
                                   org.springframework.ui.Model model) {
        model.addAttribute("flightId", flightId);
        model.addAttribute("fareCode", fareCode);
        model.addAttribute("passengers", passengers != null ? passengers : 1);
        return "passenger-details";
    }

}