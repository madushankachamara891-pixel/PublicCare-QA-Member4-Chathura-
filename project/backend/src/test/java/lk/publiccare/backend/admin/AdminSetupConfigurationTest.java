package lk.publiccare.backend.admin;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class AdminSetupConfigurationTest {

    @Test
    void configuresBcryptStrengthTwelve() {
        PasswordEncoder passwordEncoder = new AdminSetupConfiguration().adminSetupPasswordEncoder();

        BCryptPasswordEncoder bcrypt = assertInstanceOf(BCryptPasswordEncoder.class, passwordEncoder);
        assertTrue(bcrypt.encode("local-test-password").matches("^\\$2[aby]\\$12\\$.*"));
    }
}
