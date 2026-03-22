package central_api.central_api.client;

import central_api.central_api.dto.response.ValidateTokenResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "auth-api", url = "${auth.api.url:http://localhost:8083}")
public interface AuthApiClient {

    @PostMapping("/api/auth/validate")
    ValidateTokenResponse validateToken(@RequestHeader("Authorization") String authHeader);
}