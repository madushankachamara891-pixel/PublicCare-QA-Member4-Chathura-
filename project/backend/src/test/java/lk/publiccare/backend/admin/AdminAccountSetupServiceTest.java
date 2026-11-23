package lk.publiccare.backend.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import lk.publiccare.backend.user.PublicCareUser;
import lk.publiccare.backend.user.UserRepository;
import lk.publiccare.backend.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminAccountSetupServiceTest {
    @Mock UserRepository users;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private AdminAccountSetupService service;

    @BeforeEach
    void setUp() {
        service = new AdminAccountSetupService(users, encoder);
    }

    @Test
    void createsFirstAdminWithNormalizedEmailAndEncodedPassword() {
        when(users.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(users.existsByRole(UserRole.ADMIN)).thenReturn(false);
        when(users.saveAndFlush(any(PublicCareUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(AdminAccountSetupService.SetupResult.CREATED,
                service.createFirstAdmin(" Local Admin ", "Admin@Example.com", "a-long-local-password"));

        ArgumentCaptor<PublicCareUser> captor = ArgumentCaptor.forClass(PublicCareUser.class);
        verify(users).saveAndFlush(captor.capture());
        PublicCareUser created = captor.getValue();
        assertEquals("Local Admin", created.getFullName());
        assertEquals("admin@example.com", created.getEmail());
        assertEquals(UserRole.ADMIN, created.getRole());
        assertNotEquals("a-long-local-password", created.getPasswordHash());
        org.junit.jupiter.api.Assertions.assertTrue(encoder.matches("a-long-local-password", created.getPasswordHash()));
    }

    @Test
    void rerunningForExistingAdminDoesNotChangeCredentialsOrRole() {
        when(users.findByEmail("admin@example.com")).thenReturn(Optional.of(
                new PublicCareUser("Existing Admin", "admin@example.com", "existing-hash", UserRole.ADMIN)));

        assertEquals(AdminAccountSetupService.SetupResult.ALREADY_CONFIGURED,
                service.createFirstAdmin("Changed Name", "admin@example.com", "replacement-password"));
        verify(users, never()).saveAndFlush(any());
        verify(users, never()).existsByRole(any());
    }

    @Test
    void refusesCitizenEmailAndDoesNotPromoteOrChangeIt() {
        when(users.findByEmail("citizen@example.com")).thenReturn(Optional.of(
                new PublicCareUser("Citizen", "citizen@example.com", "existing-hash", UserRole.CITIZEN)));

        assertThrows(IllegalStateException.class, () -> service.createFirstAdmin(
                "Admin", "citizen@example.com", "a-long-local-password"));
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void refusesToCreateAnotherAdminWhenOneAlreadyExists() {
        when(users.findByEmail("new-admin@example.com")).thenReturn(Optional.empty());
        when(users.existsByRole(UserRole.ADMIN)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.createFirstAdmin(
                "Admin", "new-admin@example.com", "a-long-local-password"));
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void reportsMissingCredentialFieldsWithoutIncludingValuesOrAccessingDatabase() {
        var error = assertThrows(IllegalArgumentException.class,
                () -> service.createFirstAdmin(null, null, null));

        assertEquals("Admin setup input is invalid: administrator full name is missing; "
                + "administrator email is missing; administrator password is missing.", error.getMessage());
        verifyNoRepositoryInteractions();
    }

    @Test
    void reportsMissingPasswordWithoutIncludingCredentialsOrAccessingDatabase() {
        var error = assertThrows(IllegalArgumentException.class,
                () -> service.createFirstAdmin("Local Admin", "admin@example.com", null));

        assertEquals("Admin setup input is invalid: administrator password is missing.", error.getMessage());
        verifyNoRepositoryInteractions();
    }

    private void verifyNoRepositoryInteractions() {
        verify(users, never()).findByEmail(any());
        verify(users, never()).existsByRole(any());
        verify(users, never()).saveAndFlush(any());
    }
}
