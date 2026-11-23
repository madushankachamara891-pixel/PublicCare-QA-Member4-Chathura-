package lk.publiccare.backend.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import lk.publiccare.backend.user.PublicCareUser;
import lk.publiccare.backend.user.UserRepository;
import lk.publiccare.backend.user.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock UserRepository users;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(users, encoder);
    }

    @Test
    void registrationNormalizesEmailHashesPasswordAndForcesCitizenRole() {
        when(users.existsByEmail("citizen@example.com")).thenReturn(false);
        when(users.saveAndFlush(any(PublicCareUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.register(new AuthDtos.RegisterRequest(" Citizen ", "Citizen@Example.com", "a-long-test-password"));

        ArgumentCaptor<PublicCareUser> captor = ArgumentCaptor.forClass(PublicCareUser.class);
        verify(users).saveAndFlush(captor.capture());
        PublicCareUser saved = captor.getValue();
        assertEquals("citizen@example.com", saved.getEmail());
        assertEquals("Citizen", saved.getFullName());
        assertEquals(UserRole.CITIZEN, saved.getRole());
        assertNotEquals("a-long-test-password", saved.getPasswordHash());
        assertEquals(true, encoder.matches("a-long-test-password", saved.getPasswordHash()));
        assertEquals(UserRole.CITIZEN, response.role());
    }

    @Test
    void duplicateEmailIsRejectedBeforeSaving() {
        when(users.existsByEmail("taken@example.com")).thenReturn(true);
        assertThrows(DuplicateEmailException.class, () -> authService.register(
                new AuthDtos.RegisterRequest("Citizen", "Taken@example.com", "a-long-test-password")));
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void passwordExceedingBcryptByteLimitIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> authService.register(
                new AuthDtos.RegisterRequest("Citizen", "citizen@example.com", "é".repeat(37))));
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void authenticationRequestToStringRedactsPassword() {
        assertFalse(new AuthDtos.LoginRequest("citizen@example.com", "never-log-this-password")
                .toString().contains("never-log-this-password"));
        assertFalse(new AuthDtos.RegisterRequest("Citizen", "citizen@example.com", "never-log-this-password")
                .toString().contains("never-log-this-password"));
    }
}
