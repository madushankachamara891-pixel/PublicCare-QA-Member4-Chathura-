package lk.publiccare.backend.report;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import lk.publiccare.backend.config.WebSecurityConfig;
import lk.publiccare.backend.security.PublicCarePrincipal;
import lk.publiccare.backend.user.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@WebMvcTest({ReportController.class, AdminReportController.class, PublicReportController.class})
@Import(WebSecurityConfig.class)
@ExtendWith(OutputCaptureExtension.class)
class ReportControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean ReportService service;
    @MockitoBean ReportImageService imageService;
    @MockitoBean ReportProgressService progressService;
    @MockitoBean ReportDeletionService deletionService;
    @MockitoBean PublicReportService publicReportService;

    private UsernamePasswordAuthenticationToken citizen() {
        return UsernamePasswordAuthenticationToken.authenticated(new PublicCarePrincipal(42L, "citizen@example.com", "unused", "CITIZEN"), null, List.of());
    }

    private UsernamePasswordAuthenticationToken admin() {
        PublicCarePrincipal principal = new PublicCarePrincipal(7L, "admin@example.com", "unused", "ADMIN");
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
    }

    @Test
    void anonymousVisitorCanReadOnlyMinimizedPublicReportFields() throws Exception {
        when(publicReportService.list(0, 9)).thenReturn(new PublicReportDtos.Page(List.of(
                new PublicReportDtos.Report(51L, "Damaged road", "Road damage", ReportStatus.IN_PROGRESS,
                        "Galle", "A large pothole needs repair.", java.time.Instant.parse("2026-10-31T00:00:00Z"), List.of(
                        new PublicReportDtos.ProgressUpdate(ReportStatus.IN_PROGRESS,
                                java.time.Instant.parse("2026-10-31T01:00:00Z"), "Repair crew assigned.")))), 0, 9, 1, 1));

        mvc.perform(get("/api/public/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].category").value("Road damage"))
                .andExpect(jsonPath("$.content[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.content[0].location").value("Galle"))
                .andExpect(jsonPath("$.content[0].progressUpdates[0].note").value("Repair crew assigned."))
                .andExpect(jsonPath("$.content[0].ownerId").doesNotExist())
                .andExpect(jsonPath("$.content[0].ownerName").doesNotExist())
                .andExpect(jsonPath("$.content[0].progressUpdates[0].imageUrl").doesNotExist())
                .andExpect(jsonPath("$.content[0].progressUpdates[0].storageKey").doesNotExist());
        verify(publicReportService).list(0, 9);
    }

    @Test
    void anonymousVisitorCanReadPublicReportDetailsButNotPrivateImages() throws Exception {
        when(publicReportService.detail(51L)).thenReturn(new PublicReportDtos.Report(51L, "Damaged road",
                "Road damage", ReportStatus.IN_PROGRESS, "Galle", "A large pothole needs repair.",
                java.time.Instant.parse("2026-10-31T00:00:00Z"), List.of()));

        mvc.perform(get("/api/public/reports/51"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Damaged road"))
                .andExpect(jsonPath("$.ownerId").doesNotExist())
                .andExpect(jsonPath("$.adminUpdateNote").doesNotExist());
        verify(publicReportService).detail(51L);
        mvc.perform(get("/api/public/reports/51/image")).andExpect(status().isUnauthorized());
    }

    @Test
    void onlyExactPublicReportGetIsAnonymousAndPhotoPathsRemainProtected() throws Exception {
        mvc.perform(get("/api/public/reports/51/image")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/reports/51/image")).andExpect(status().isUnauthorized());
        org.mockito.Mockito.verifyNoInteractions(imageService, publicReportService);
    }

    @Test
    void publicApprovalRoutesRemainAdminOnlyAndRequireCsrf() throws Exception {
        when(service.setPublicApproval(51L, true, 7L)).thenReturn(true);
        mvc.perform(patch("/api/admin/reports/51/public-approval").with(csrf()).with(authentication(admin()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"approved\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.approved").value(true));
        verify(service).setPublicApproval(51L, true, 7L);

        mvc.perform(patch("/api/admin/reports/51/public-approval").with(csrf()).with(authentication(citizen()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"approved\":true}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/admin/reports/51/public-approval").with(authentication(admin()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"approved\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void progressAndNoteApprovalsAreSeparateAdminOnlyActions() throws Exception {
        when(progressService.setPublicApproval(51L, 8L, true, 7L)).thenReturn(true);
        when(progressService.setNotePublicApproval(51L, 8L, true, 7L)).thenReturn(true);

        mvc.perform(patch("/api/admin/reports/51/progress-updates/8/public-approval").with(csrf()).with(authentication(admin()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"approved\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.approved").value(true));
        mvc.perform(patch("/api/admin/reports/51/progress-updates/8/note-public-approval").with(csrf()).with(authentication(admin()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"approved\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.approved").value(true));

        verify(progressService).setPublicApproval(51L, 8L, true, 7L);
        verify(progressService).setNotePublicApproval(51L, 8L, true, 7L);
        mvc.perform(patch("/api/admin/reports/51/progress-updates/8/note-public-approval").with(csrf()).with(authentication(citizen()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"approved\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createTakesOwnerFromPrincipalAndIgnoresSubmittedOwnerId() throws Exception {
        when(service.create(eq(42L), any())).thenReturn(new ReportDtos.ReportResponse(3L, "PC-00000003", "Title", "Description", "Road damage", "Galle", ReportStatus.PENDING, null, 42L, "Citizen", null, null));
        mvc.perform(post("/api/reports").with(csrf()).with(authentication(citizen())).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Title\",\"description\":\"Description\",\"category\":\"Road damage\",\"location\":\"Galle\",\"ownerId\":999}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.ownerId").value(42)).andExpect(jsonPath("$.reference").value("PC-00000003"));
        verify(service).create(eq(42L), any(ReportDtos.CreateRequest.class));
    }

    @Test
    void reportInputValidationRejectsMissingRequiredValues() throws Exception {
        mvc.perform(post("/api/reports").with(csrf()).with(authentication(citizen())).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\" \",\"description\":\"\",\"category\":\"\",\"location\":\"\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").exists());
    }

    @Test
    void reportWritesRequireCsrf() throws Exception {
        mvc.perform(post("/api/reports").with(authentication(citizen())).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Title\",\"description\":\"Description\",\"category\":\"Road damage\",\"location\":\"Galle\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonOwnedOrMissingReportUsesSafeNotFoundResponse() throws Exception {
        when(service.mineDetail(42L, 999L)).thenThrow(new ReportNotFoundException());
        mvc.perform(get("/api/reports/999").with(authentication(citizen())))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("Report not found."));
        verify(service).mineDetail(42L, 999L);
    }

    @Test
    void ownerCanReadAdminUpdateNoteFromReportDetails() throws Exception {
        when(service.mineDetail(42L, 51L)).thenReturn(new ReportDtos.ReportResponse(51L, "PC-00000051",
                "Broken bridge", "The bridge is damaged.", "Road damage", "Galle", ReportStatus.RESOLVED,
                "The bridge has been repaired.", 42L, "Citizen", null, null));

        mvc.perform(get("/api/reports/51").with(authentication(citizen())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.adminUpdateNote").value("The bridge has been repaired."))
                .andExpect(jsonPath("$.publicApproved").doesNotExist());

        verify(service).mineDetail(42L, 51L);
    }

    @Test
    void reportDetailsRequireAuthentication() throws Exception {
        mvc.perform(get("/api/reports/51").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void citizenCanUploadAndRetrieveOwnReportImageWithCsrf() throws Exception {
        MockMultipartFile image = new MockMultipartFile("image", "photo.png", "image/png", new byte[] {1, 2, 3});
        when(imageService.citizenImage(42L, 51L)).thenReturn(new ReportImageService.ImageContent(new byte[] {1, 2, 3}, "image/png"));

        mvc.perform(multipart("/api/reports/51/image").file(image).with(csrf()).with(authentication(citizen())))
                .andExpect(status().isNoContent());
        org.mockito.Mockito.verify(imageService).upload(eq(42L), eq(51L), any());

        mvc.perform(get("/api/reports/51/image").with(authentication(citizen())))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentType("image/png"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().bytes(new byte[] {1, 2, 3}));
        org.mockito.Mockito.verify(imageService).citizenImage(42L, 51L);
    }

    @Test
    void imageUploadRequiresAuthenticationAndCsrf() throws Exception {
        MockMultipartFile image = new MockMultipartFile("image", "photo.png", "image/png", new byte[] {1, 2, 3});
        mvc.perform(multipart("/api/reports/51/image").file(image).with(authentication(citizen())))
                .andExpect(status().isForbidden());
        mvc.perform(multipart("/api/reports/51/image").file(image).with(csrf()))
                .andExpect(status().isUnauthorized());
        org.mockito.Mockito.verifyNoInteractions(imageService);
    }

    @Test
    void onlyAdminsCanRetrieveAnImageThroughAdminRoute() throws Exception {
        when(imageService.adminImage(51L)).thenReturn(new ReportImageService.ImageContent(new byte[] {4}, "image/jpeg"));
        mvc.perform(get("/api/admin/reports/51/image").with(user("citizen@example.com").roles("CITIZEN")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/reports/51/image").with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentType("image/jpeg"));
        org.mockito.Mockito.verify(imageService).adminImage(51L);
    }

    @Test
    void ownerCanUpdateTheirReport() throws Exception {
        when(service.updateMine(eq(42L), eq(51L), any())).thenReturn(new ReportDtos.ReportResponse(51L,
                "PC-00000051", "Updated title", "Updated description", "Road damage", "Galle",
                ReportStatus.PENDING, null, 42L, "Citizen", null, null));

        mvc.perform(put("/api/reports/51").with(csrf()).with(authentication(citizen()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated title\",\"description\":\"Updated description\",\"category\":\"Road damage\",\"location\":\"Galle\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated title"));

        verify(service).updateMine(eq(42L), eq(51L), any(ReportDtos.UpdateRequest.class));
    }

    @Test
    void differentCitizenCannotUpdateReportAndGetsSafeNotFound() throws Exception {
        when(service.updateMine(eq(73L), eq(51L), any())).thenThrow(new ReportNotFoundException());

        mvc.perform(put("/api/reports/51").with(csrf())
                        .with(authentication(UsernamePasswordAuthenticationToken.authenticated(
                                new PublicCarePrincipal(73L, "other@example.com", "unused", "CITIZEN"), null, List.of())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Attempted change\",\"description\":\"Changed\",\"category\":\"Road damage\",\"location\":\"Galle\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Report not found."));

        verify(service).updateMine(eq(73L), eq(51L), any(ReportDtos.UpdateRequest.class));
    }

    @Test
    void reportUpdatesRequireAuthentication() throws Exception {
        mvc.perform(put("/api/reports/51").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated title\",\"description\":\"Updated description\",\"category\":\"Road damage\",\"location\":\"Galle\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminReportApiRequiresAdminRole() throws Exception {
        mvc.perform(get("/api/admin/reports").with(user("citizen@example.com").roles("CITIZEN"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/reports").with(user("admin@example.com").roles("ADMIN"))).andExpect(status().isOk());
    }

    @Test
    void onlyAuthenticatedAdminsCanDeleteReportsAndDeleteRequiresCsrf() throws Exception {
        mvc.perform(delete("/api/admin/reports/51").with(csrf()).with(authentication(admin())))
                .andExpect(status().isNoContent());
        verify(deletionService).deleteAdmin(51L);

        org.mockito.Mockito.reset(deletionService);
        mvc.perform(delete("/api/admin/reports/51").with(csrf()))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/admin/reports/51").with(csrf()).with(authentication(citizen())))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/admin/reports/51").with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isForbidden());
        org.mockito.Mockito.verifyNoInteractions(deletionService);
    }

    @Test
    void deleteFailureIsLoggedServerSideAndReturnsGenericError(CapturedOutput output) throws Exception {
        org.mockito.Mockito.doThrow(new IllegalStateException("sensitive database detail"))
                .when(deletionService).deleteAdmin(51L);

        mvc.perform(delete("/api/admin/reports/51").with(csrf()).with(authentication(admin())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Unable to delete report."))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("sensitive database detail"))));

        assertThat(output).contains("Admin report deletion failed for report 51")
                .contains("IllegalStateException").contains("sensitive database detail");
    }

    @Test
    void citizenCannotUpdateAdminReportStatus() throws Exception {
        mvc.perform(patch("/api/admin/reports/51/status").with(csrf())
                        .with(user("citizen@example.com").roles("CITIZEN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\",\"adminUpdateNote\":\"Done\"}"))
                .andExpect(status().isForbidden());

        org.mockito.Mockito.verifyNoInteractions(service);
    }

    @Test
    void adminCanUpdateReportStatusAndPatchRequiresCsrf() throws Exception {
        when(service.updateStatus(eq(51L), any())).thenReturn(new AdminReportDtos.ReportResponse(51L,
                "PC-00000051", "Broken bridge", "The bridge is damaged.", "Road damage", "Galle",
                ReportStatus.RESOLVED, "Repaired", 42L, "Citizen", null, null, false));

        mvc.perform(patch("/api/admin/reports/51/status").with(csrf())
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\",\"adminUpdateNote\":\"Repaired\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.adminUpdateNote").value("Repaired"));

        verify(service).updateStatus(eq(51L), any(ReportDtos.StatusRequest.class));

        org.mockito.Mockito.reset(service);
        mvc.perform(patch("/api/admin/reports/51/status")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\",\"adminUpdateNote\":\"Repaired\"}"))
                .andExpect(status().isForbidden());
        org.mockito.Mockito.verifyNoInteractions(service);
    }

    @Test
    void adminReportDetailsIncludeCurrentPublicApprovalState() throws Exception {
        when(service.adminDetail(51L)).thenReturn(new AdminReportDtos.ReportResponse(51L,
                "PC-00000051", "Broken bridge", "The bridge is damaged.", "Road damage", "Galle",
                ReportStatus.IN_PROGRESS, null, 42L, "Citizen", null, null, true));

        mvc.perform(get("/api/admin/reports/51").with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicApproved").value(true));
    }

    @Test
    void progressHistoryIsOwnerScopedAndAdminRoleProtected() throws Exception {
        when(progressService.citizenList(42L, 51L)).thenReturn(List.of(new ReportDtos.ProgressUpdateResponse(8L, ReportStatus.IN_PROGRESS, "Crew assigned", java.time.Instant.parse("2026-10-31T00:00:00Z"), false)));
        when(progressService.adminList(51L)).thenReturn(List.of(new AdminReportDtos.ProgressUpdateResponse(8L, ReportStatus.IN_PROGRESS, "Crew assigned", java.time.Instant.parse("2026-10-31T00:00:00Z"), false, true, false)));
        mvc.perform(get("/api/reports/51/progress-updates").with(authentication(citizen())))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("IN_PROGRESS"));
        verify(progressService).citizenList(42L, 51L);
        mvc.perform(get("/api/admin/reports/51/progress-updates").with(user("citizen@example.com").roles("CITIZEN"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/reports/51/progress-updates").with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].publicApproved").value(true))
                .andExpect(jsonPath("$[0].notePublicApproved").value(false));
        verify(progressService).adminList(51L);
    }

    @Test
    void adminProgressWriteRequiresAdminAndCsrfAndSavesOptionalImageInOneRequest() throws Exception {
        var image = new MockMultipartFile("image", "proof.png", "image/png", new byte[] {1, 2, 3});
        when(progressService.create(eq(51L), eq(ReportStatus.RESOLVED), eq("Fixed"), any())).thenReturn(new AdminReportDtos.ProgressUpdateResponse(11L, ReportStatus.RESOLVED, "Fixed", java.time.Instant.parse("2026-10-31T00:00:00Z"), true, false, false));
        mvc.perform(multipart("/api/admin/reports/51/progress-updates").file(image).param("status", "RESOLVED").param("note", "Fixed").with(csrf()).with(user("citizen@example.com").roles("CITIZEN"))).andExpect(status().isForbidden());
        mvc.perform(multipart("/api/admin/reports/51/progress-updates").param("status", "RESOLVED").param("note", "Fixed").with(user("admin@example.com").roles("ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(multipart("/api/admin/reports/51/progress-updates").file(image).param("status", "RESOLVED").param("note", "Fixed").with(csrf()).with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("RESOLVED")).andExpect(jsonPath("$.hasImage").value(true));
        verify(progressService).create(eq(51L), eq(ReportStatus.RESOLVED), eq("Fixed"), any());
    }
}
