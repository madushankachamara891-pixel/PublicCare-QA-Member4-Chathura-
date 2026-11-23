package lk.publiccare.backend.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import lk.publiccare.backend.config.WebSecurityConfig;
import lk.publiccare.backend.admin.AdminSetupConfiguration;
import lk.publiccare.backend.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(AuthController.class)
@Import({WebSecurityConfig.class, AuthControllerTest.AdminConfigurationComponentScan.class})
class AuthControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired AuthenticationConfiguration authenticationConfiguration;
    @MockitoBean AuthService authService;
    @MockitoBean AuthenticationManager authenticationManager;
    @MockitoBean SessionAuthenticationStrategy sessionAuthenticationStrategy;

    /** Mirrors the main application's package scan for admin configuration classes. */
    @TestConfiguration(proxyBeanMethods = false)
    @ComponentScan(basePackageClasses = AdminSetupConfiguration.class, useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION,
                    classes = org.springframework.context.annotation.Configuration.class))
    static class AdminConfigurationComponentScan { }

    @Test
    void normalStartupRetainsSpringSecurityAuthenticationConfiguration() {
        assertNotNull(authenticationConfiguration);
    }

    private AuthDtos.UserResponse citizen() {
        return new AuthDtos.UserResponse(17L, "Test Citizen", "citizen@example.com", UserRole.CITIZEN, Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void registrationReturnsCreatedAccountAndNeverTakesPublicRole() throws Exception {
        when(authService.register(any())).thenReturn(citizen());

        mockMvc.perform(post("/api/auth/register").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Test Citizen","email":"citizen@example.com","password":"long-test-password","role":"ADMIN"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CITIZEN"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        verify(authService).register(any(AuthDtos.RegisterRequest.class));
    }

    @Test
    void invalidRegistrationIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/register").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"","email":"bad","password":"short"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void stateChangingAuthRequestsRequireCsrfToken() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Test Citizen","email":"citizen@example.com","password":"long-test-password"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginPersistsSessionAndMeUsesThatSession() throws Exception {
        when(authenticationManager.authenticate(any())).thenReturn(UsernamePasswordAuthenticationToken.authenticated(
                "citizen@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_CITIZEN"))));
        when(authService.findResponse("citizen@example.com")).thenReturn(citizen());

        MvcResult login = mockMvc.perform(post("/api/auth/login").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"citizen@example.com","password":"long-test-password"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(get("/api/auth/me").session((org.springframework.mock.web.MockHttpSession) login.getRequest().getSession(false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("citizen@example.com"));
    }

    @Test
    void invalidLoginReturnsGenericUnauthorizedMessage() throws Exception {
        when(authenticationManager.authenticate(any())).thenThrow(new org.springframework.security.authentication.BadCredentialsException("secret detail"));
        mockMvc.perform(post("/api/auth/login").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"citizen@example.com","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password."));
    }

    @Test
    void protectedAndAdminRoutesEnforceAuthenticationAndRole() throws Exception {
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/issues").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("citizen@example.com").roles("CITIZEN")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/issues").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }
}
