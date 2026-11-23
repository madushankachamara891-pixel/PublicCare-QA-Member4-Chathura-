package lk.publiccare.backend.security;

import lk.publiccare.backend.user.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private final UserRepository users;

    public DatabaseUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var user = users.findByEmail(email.toLowerCase(java.util.Locale.ROOT))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new PublicCarePrincipal(user.getId(), user.getEmail(), user.getPasswordHash(), user.getRole().name());
    }
}
