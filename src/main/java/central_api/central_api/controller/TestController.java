package central_api.central_api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/hash/{password}")
    public String getHash(@PathVariable String password) {
        return passwordEncoder.encode(password);
    }

    @GetMapping("/check/{password}/{hash}")
    public String checkPassword(@PathVariable String password, @PathVariable String hash) {
        boolean matches = passwordEncoder.matches(password, hash);
        return "Password matches: " + matches;
    }
}