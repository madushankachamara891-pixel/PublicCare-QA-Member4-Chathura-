package lk.publiccare.backend.auth;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import lk.publiccare.backend.user.PublicCareUser;
import lk.publiccare.backend.user.UserRepository;
import lk.publiccare.backend.user.UserRole;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthDtos.UserResponse register(AuthDtos.RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password must be at most 72 UTF-8 bytes.");
        }
        if (users.existsByEmail(email)) throw new DuplicateEmailException();
        var user = new PublicCareUser(request.fullName().trim(), email,
                passwordEncoder.encode(request.password()), UserRole.CITIZEN);
        try {
            return response(users.saveAndFlush(user));
        } catch (DataIntegrityViolationException ex) {
            // A concurrent registration can win the unique-email race after the pre-check.
            throw new DuplicateEmailException();
        }
    }

    public AuthDtos.UserResponse findResponse(String email) {
        return users.findByEmail(email.toLowerCase(Locale.ROOT))
                .map(AuthService::response)
                .orElseThrow(() -> new IllegalStateException("Authenticated user record is unavailable."));
    }

    private static AuthDtos.UserResponse response(PublicCareUser user) {
        return new AuthDtos.UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }
}
