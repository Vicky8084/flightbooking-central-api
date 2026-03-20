package central_api.central_api.security;

import central_api.central_api.client.DbApiClient;
import central_api.central_api.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final DbApiClient dbApiClient;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        try {
            UserResponse user = dbApiClient.getUserByEmail(email);

            return User.builder()
                    .username(user.getEmail())
                    .password("") // Password will be validated by Auth API
                    .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole())))
                    .build();
        } catch (Exception e) {
            throw new UsernameNotFoundException("User not found with email: " + email);
        }
    }
}
