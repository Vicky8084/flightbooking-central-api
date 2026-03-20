package central_api.central_api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


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
        return "search-flights";  // search-flights.html
    }

    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "forgot-password";
    }

    @GetMapping("/system-admin-login")
    public String systemAdminLogin() {
        return "system-admin-login";  // system-admin-login.html
    }

    @GetMapping("/system-dashboard")
    public String systemDashboard() {
        return "system-dashboard";
    }

}
