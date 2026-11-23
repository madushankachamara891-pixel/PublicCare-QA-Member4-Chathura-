package lk.publiccare.backend.admin;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;

import lk.publiccare.backend.user.PublicCareUser;
import lk.publiccare.backend.user.UserRepository;
import lk.publiccare.backend.user.UserRole;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAccountSetupService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AdminAccountSetupService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public SetupResult createFirstAdmin(String fullName, String email, String password) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        String normalizedName = fullName == null ? "" : fullName.trim();
        validateCredentials(normalizedName, normalizedEmail, password);

        var existingUser = users.findByEmail(normalizedEmail);
        if (existingUser.isPresent()) {
            if (existingUser.get().getRole() == UserRole.ADMIN) return SetupResult.ALREADY_CONFIGURED;
            throw new IllegalStateException("The requested email already belongs to a non-admin account.");
        }
        if (users.existsByRole(UserRole.ADMIN)) {
            throw new IllegalStateException("An administrator already exists; setup did not change any account.");
        }

        users.saveAndFlush(new PublicCareUser(normalizedName, normalizedEmail,
                passwordEncoder.encode(password), UserRole.ADMIN));
        return SetupResult.CREATED;
    }

    private static void validateCredentials(String fullName, String email, String password) {
        var problems = new ArrayList<String>();
        if (fullName.isEmpty()) problems.add("administrator full name is missing");
        else if (fullName.length() > 120) problems.add("administrator full name exceeds 120 characters");
        if (email.isEmpty()) problems.add("administrator email is missing");
        else if (email.length() > 254) problems.add("administrator email exceeds 254 characters");
        if (password == null || password.isEmpty()) problems.add("administrator password is missing");
        else {
            if (password.length() < 12) problems.add("administrator password must be at least 12 characters");
            if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
                problems.add("administrator password exceeds 72 UTF-8 bytes");
            }
        }
        if (!problems.isEmpty()) {
            throw new IllegalArgumentException("Admin setup input is invalid: " + String.join("; ", problems) + ".");
        }
    }

    public enum SetupResult { CREATED, ALREADY_CONFIGURED }
}
