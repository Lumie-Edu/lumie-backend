package com.lumie.app;

import com.lumie.auth.application.service.AuthQueryService;
import com.lumie.file.application.service.TemporaryObjectUploadService;
import com.lumie.file.application.service.FileCommandService;
import com.lumie.student.application.service.StudentQueryService;
import com.lumie.classroom.application.service.ClassQueryService;
import com.lumie.ai.application.service.SchemaDiscoveryService;
import com.lumie.tenant.api.TenantService;
import com.lumie.content.application.port.out.AnnouncementPersistencePort;
import com.lumie.content.application.port.out.AnnouncementClassTargetPersistencePort;
import com.lumie.file.api.FileService;
import com.lumie.classroom.api.ClassService;
import com.lumie.staff.api.StaffService;
import com.lumie.staff.application.service.StaffQueryService;
import com.lumie.exam.application.service.ExamRegradeService;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockReset;

@CucumberContextConfiguration
public class CucumberSpringConfiguration extends FullContextTestBase {

    @MockitoBean(reset = MockReset.NONE)
    AuthQueryService authQueryService;

    @MockitoBean(reset = MockReset.NONE)
    TemporaryObjectUploadService temporaryObjectUploadService;

    @MockitoBean(reset = MockReset.NONE)
    FileCommandService fileCommandService;

    @MockitoBean(reset = MockReset.NONE)
    StudentQueryService studentQueryService;

    @MockitoBean(reset = MockReset.NONE)
    ClassQueryService classQueryService;

    @MockitoBean(reset = MockReset.NONE)
    SchemaDiscoveryService schemaDiscoveryService;

    @MockitoBean(reset = MockReset.NONE)
    TenantService tenantService;

    @MockitoBean(reset = MockReset.NONE)
    AnnouncementPersistencePort announcementRepository;

    @MockitoBean(reset = MockReset.NONE)
    AnnouncementClassTargetPersistencePort announcementClassTargetRepository;

    @MockitoBean(reset = MockReset.NONE)
    FileService fileService;

    @MockitoBean(reset = MockReset.NONE)
    ClassService classService;

    @MockitoBean(reset = MockReset.NONE)
    StaffService staffService;

    @MockitoBean(reset = MockReset.NONE)
    StaffQueryService staffQueryService;

    @MockitoBean(reset = MockReset.NONE)
    ExamRegradeService examRegradeService;

}
