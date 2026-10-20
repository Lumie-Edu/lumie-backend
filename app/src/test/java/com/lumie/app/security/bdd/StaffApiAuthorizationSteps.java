package com.lumie.app.security.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.clearInvocations;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

import com.lumie.auth.application.service.AuthQueryService;
import com.lumie.auth.adapter.out.config.SessionConfig;
import com.lumie.auth.domain.vo.AuthSession;
import com.lumie.auth.domain.vo.DeviceCategory;
import com.lumie.common.auth.Role;
import com.lumie.file.application.service.TemporaryObjectUploadService;
import com.lumie.file.application.service.FileCommandService;
import com.lumie.file.application.dto.request.CreateTemporaryUploadRequest;
import com.lumie.student.application.service.StudentQueryService;
import com.lumie.classroom.application.service.ClassQueryService;
import com.lumie.ai.application.service.SchemaDiscoveryService;
import com.lumie.tenant.api.TenantService;
import com.lumie.tenant.api.TenantStatus;
import com.lumie.content.domain.repository.AnnouncementRepository;
import com.lumie.content.domain.repository.AnnouncementClassTargetRepository;
import com.lumie.content.domain.entity.Announcement;
import com.lumie.file.api.FileService;
import com.lumie.classroom.api.ClassService;
import com.lumie.staff.api.StaffService;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class StaffApiAuthorizationSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthQueryService authQueryService;

    @Autowired
    private SessionConfig sessionConfig;

    @Autowired
    private TemporaryObjectUploadService temporaryObjectUploadService;

    @Autowired
    private FileCommandService fileCommandService;

    @Autowired
    private StudentQueryService studentQueryService;

    @Autowired
    private ClassQueryService classQueryService;

    @Autowired
    private SchemaDiscoveryService schemaDiscoveryService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private AnnouncementClassTargetRepository announcementClassTargetRepository;

    @Autowired
    private FileService fileService;

    @Autowired
    private ClassService classService;

    @Autowired
    private StaffService staffService;

    private Cookie sessionCookie;
    private MvcResult result;
    private String sessionId;

    @Given("an authenticated {string} session")
    public void anAuthenticatedSession(String roleName) {
        clearInvocations(authQueryService, temporaryObjectUploadService, studentQueryService, fileCommandService,
                classQueryService, schemaDiscoveryService, tenantService, announcementRepository,
                announcementClassTargetRepository, fileService, classService, staffService);
        sessionId = java.util.UUID.randomUUID().toString();
        Role role = Role.valueOf(roleName);
        Instant now = Instant.now();
        given(authQueryService.findActiveSession(sessionId)).willReturn(Optional.of(new AuthSession(
                sessionId, 10L, "Test User", "test-tenant", 20L, role, "test", DeviceCategory.UNKNOWN,
                now, now.plusSeconds(3600))));
        if (role.hasAuthority(Role.INSTRUCTOR)) {
            given(studentQueryService.searchStudents(
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any()))
                    .willReturn(org.springframework.data.domain.Page.empty());
        }
        given(classQueryService.getClassesForCurrentStudent(org.mockito.ArgumentMatchers.any()))
                .willReturn(org.springframework.data.domain.Page.empty());
        sessionCookie = new Cookie(sessionConfig.getCookieName(), sessionId);
    }

    @Given("no authenticated session")
    public void noAuthenticatedSession() {
        sessionCookie = null;
    }

    @Given("an authenticated session without a role")
    public void anAuthenticatedSessionWithoutARole() {
        clearInvocations(authQueryService, temporaryObjectUploadService, studentQueryService, fileCommandService);
        sessionId = java.util.UUID.randomUUID().toString();
        Instant now = Instant.now();
        given(authQueryService.findActiveSession(sessionId)).willReturn(Optional.of(new AuthSession(
                sessionId, 10L, "Unknown Role", "test-tenant", 20L, null, "test", DeviceCategory.UNKNOWN,
                now, now.plusSeconds(3600))));
        sessionCookie = new Cookie(sessionConfig.getCookieName(), sessionId);
    }

    @When("the client requests {string}")
    public void theClientRequests(String path) throws Exception {
        perform(get(path).header("Host", "localhost"));
    }

    @When("the client sends {string} to {string}")
    public void theClientSendsTo(String method, String path) throws Exception {
        var request = switch (method) {
            case "DELETE" -> delete(path);
            default -> throw new IllegalArgumentException("Unsupported method: " + method);
        };
        perform(request.header("Host", "localhost").header("Origin", "http://localhost"));
    }

    @When("the client requests a {string} upload")
    public void theClientRequestsUpload(String entityType) throws Exception {
        String body = """
                {"entityType":"%s","filename":"question.pdf","contentType":"application/pdf","fileSize":12}
                """.formatted(entityType);
        perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/v1/files/uploads")
                .header("Host", "localhost")
                .header("Origin", "http://localhost")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @When("the client creates an announcement with forged author id {long}")
    public void theClientCreatesAnnouncementWithForgedAuthorId(long forgedAuthorId) throws Exception {
        org.mockito.BDDMockito.given(fileService.prepareTemporaryFiles(
                        org.mockito.ArgumentMatchers.eq("announcement"), org.mockito.ArgumentMatchers.anyList()))
                .willReturn(java.util.List.of());
        org.mockito.BDDMockito.given(fileService.attachPreparedFiles(
                        org.mockito.ArgumentMatchers.eq("announcement"),
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyList()))
                .willReturn(java.util.List.of());
        org.mockito.BDDMockito.given(staffService.getStaffByUserId("test-tenant", 10L))
                .willReturn(Optional.of(new StaffService.StaffData(
                        1L, 10L, "owner_10", "Owner", null, null, true, "ACTIVE", null, null)));
        org.mockito.BDDMockito.given(announcementRepository.save(
                        org.mockito.ArgumentMatchers.any(Announcement.class)))
                .willAnswer(invocation -> {
                    Announcement announcement = invocation.getArgument(0);
                    ReflectionTestUtils.setField(announcement, "id", 1L);
                    return announcement;
                });
        String body = """
                {"authorId":%d,"announcementTitle":"Authorship","announcementContent":"Policy",
                 "isItAssetAnnouncement":false,"isItImportantAnnouncement":false,
                 "temporaryAttachments":[],"classIds":[],"audienceScope":"ALL"}
                """.formatted(forgedAuthorId);
        perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/v1/announcements")
                .header("Host", "localhost")
                .header("Origin", "http://localhost")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @When("an internal HMAC caller requests the schema for {string}")
    public void anInternalHmacCallerRequestsSchema(String tenantSlug) throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        String signature = signature(timestamp, tenantSlug, new byte[0]);
        given(tenantService.getTenantBySlug(tenantSlug)).willReturn(Optional.of(new TenantService.TenantData(
                20L, tenantSlug, "Test Tenant", TenantStatus.ACTIVE, null, null, null)));
        given(schemaDiscoveryService.getSchemaDescription(tenantSlug)).willReturn("test schema");
        perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/internal/chatbot/schema")
                .param("tenantSlug", tenantSlug)
                .header("X-Tenant-Slug", tenantSlug)
                .header("X-Timestamp", Long.toString(timestamp))
                .header("X-Signature", signature));
    }

    private static String signature(long timestamp, String tenantSlug, byte[] body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
                "test-hmac-secret-for-full-context-tests".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        mac.update(Long.toString(timestamp).getBytes(StandardCharsets.UTF_8));
        mac.update((byte) '\n');
        mac.update(tenantSlug.getBytes(StandardCharsets.UTF_8));
        mac.update((byte) '\n');
        return HexFormat.of().formatHex(mac.doFinal(body));
    }

    private void perform(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
            throws Exception {
        if (sessionCookie != null) {
            request.cookie(sessionCookie);
        }
        result = mockMvc.perform(request).andReturn();
        if (sessionCookie != null) {
            verify(authQueryService).findActiveSession(sessionId);
        }
    }

    @Then("the response status is {int}")
    public void theResponseStatusIs(int status) {
        assertThat(result).isNotNull();
        assertThat(result.getResponse().getStatus()).isEqualTo(status);
    }

    @Then("the error code is {string}")
    public void theErrorCodeIs(String code) throws Exception {
        assertThat(result.getResponse().getContentAsString()).contains(code);
        assertThat(result.getResponse().getContentType()).contains("application/problem+json");
    }

    @Then("the announcement author id is {long}")
    public void theAnnouncementAuthorIdIs(long authorId) throws Exception {
        assertThat(result.getResponse().getContentAsString()).contains("\"authorId\":" + authorId);
    }

    @Then("the upload service is called")
    public void theUploadServiceIsCalled() {
        verify(temporaryObjectUploadService).create(org.mockito.ArgumentMatchers.any(CreateTemporaryUploadRequest.class));
    }

    @Then("the upload service is not called")
    public void theUploadServiceIsNotCalled() {
        org.mockito.Mockito.verifyNoInteractions(temporaryObjectUploadService);
    }

    @Then("the file deletion service is called")
    public void theFileDeletionServiceIsCalled() {
        verify(fileCommandService).deleteFile(org.mockito.ArgumentMatchers.any(java.util.UUID.class));
    }

    @Then("the file deletion service is not called")
    public void theFileDeletionServiceIsNotCalled() {
        org.mockito.Mockito.verifyNoInteractions(fileCommandService);
    }

    @Then("the student query service is called")
    public void theStudentQueryServiceIsCalled() {
        verify(studentQueryService).searchStudents(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Then("the class query service is called")
    public void theClassQueryServiceIsCalled() {
        verify(classQueryService).getClassesForCurrentStudent(org.mockito.ArgumentMatchers.any());
    }

    @Then("the schema service is called")
    public void theSchemaServiceIsCalled() {
        verify(schemaDiscoveryService).getSchemaDescription("test-tenant");
    }
}
