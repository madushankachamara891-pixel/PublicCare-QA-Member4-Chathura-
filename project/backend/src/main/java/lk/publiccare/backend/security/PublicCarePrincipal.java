package lk.publiccare.backend.security;

import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

/** The authenticated database identity retained in the server-side session. */
public final class PublicCarePrincipal extends User {
    private final Long userId;

    public PublicCarePrincipal(Long userId, String email, String passwordHash, String role) {
        super(email, passwordHash, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        this.userId = userId;
    }

    public Long getUserId() { return userId; }
}
