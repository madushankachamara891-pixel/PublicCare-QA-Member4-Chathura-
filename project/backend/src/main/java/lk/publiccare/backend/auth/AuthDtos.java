package lk.publiccare.backend.auth;

import java.time.Instant;

import lk.publiccare.backend.user.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() { }

    public record RegisterRequest(
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 12, max = 72) String password) {
        @Override
        public String toString() {
            return "RegisterRequest[fullName=" + fullName + ", email=" + email + ", password=[REDACTED]]";
        }
    }

    public record LoginRequest(@NotBlank @Email @Size(max = 254) String email,
                               @NotBlank @Size(max = 72) String password) {
        @Override
        public String toString() {
            return "LoginRequest[email=" + email + ", password=[REDACTED]]";
        }
    }

    public record UserResponse(Long id, String fullName, String email, UserRole role, Instant createdAt) { }
    public record CsrfResponse(String token) { }
    public record ErrorResponse(String error) { }
}
