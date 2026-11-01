package com.lumie.assignment.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.lumie.assignment.domain.entity.AssignmentSubmission;
import com.lumie.assignment.domain.vo.SubmissionStatus;
import com.lumie.common.tenant.TenantContextHolder;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Tag("integration")
@Testcontainers
@DataJpaTest(excludeAutoConfiguration = FlywayAutoConfiguration.class, properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect"
})
@ContextConfiguration(classes = AssignmentPersistenceTestApplication.class)
@Import(AssignmentSubmissionPersistenceAdapter.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AssignmentSubmissionPersistenceAdapterTest {
    @Container static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine").withInitScript("assignment-test-runtime-role.sql");
    @DynamicPropertySource static void dataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", () -> "lumie_app");
        registry.add("spring.datasource.password", () -> "lumie_app");
    }

    private final AssignmentSubmissionJpaRepository repository;
    private final AssignmentSubmissionPersistenceAdapter adapter;
    private final JdbcTemplate jdbcTemplate;
    private final EntityManager entityManager;

    AssignmentSubmissionPersistenceAdapterTest(AssignmentSubmissionJpaRepository repository,
            AssignmentSubmissionPersistenceAdapter adapter, JdbcTemplate jdbcTemplate, EntityManager entityManager) {
        this.repository = repository;
        this.adapter = adapter;
        this.jdbcTemplate = jdbcTemplate;
        this.entityManager = entityManager;
    }

    @AfterEach void clearTenant() { TenantContextHolder.clear(); }

    @Test void submissionQueriesKeepPaginationFiltersCountsAndEmptyResults() {
        TenantContextHolder.setTenantId(11L);
        AssignmentPersistenceTestApplication.enableTenantRls(jdbcTemplate, "assignment_submissions");
        AssignmentSubmission pending = repository.save(AssignmentSubmission.create(101L, 7L));
        AssignmentSubmission graded = AssignmentSubmission.create(101L, 8L);
        graded.submitExam(Map.of("q1", "a"), 900L, 80);
        graded = repository.save(graded);
        AssignmentSubmission other = repository.save(AssignmentSubmission.create(102L, 7L));
        repository.flush();

        assertThat(adapter.findByAssignmentId(101L, PageRequest.of(0, 1)).getTotalElements()).isEqualTo(2);
        assertThat(adapter.findByAssignmentId(999L, PageRequest.of(0, 10)).getContent()).isEmpty();
        assertThat(adapter.findAllByAssignmentId(101L)).hasSize(2);
        assertThat(adapter.findAllByAssignmentIdIn(List.of(101L, 102L))).hasSize(3);
        assertThat(adapter.findAllByAssignmentIdIn(List.of())).isEmpty();
        assertThat(adapter.findByStudentId(7L, PageRequest.of(0, 10)).getTotalElements()).isEqualTo(2);
        assertThat(adapter.findByStudentId(999L, PageRequest.of(0, 10)).getContent()).isEmpty();
        assertThat(adapter.findByAssignmentIdAndStudentId(101L, 8L, PageRequest.of(0, 10)).getContent())
                .containsExactly(graded);
        assertThat(adapter.findByAssignmentIdAndStudentId(101L, 999L, PageRequest.of(0, 10)).getContent()).isEmpty();
        assertThat(adapter.findFirstByAssignmentIdAndStudentId(101L, 7L)).contains(pending);
        assertThat(adapter.findFirstByAssignmentIdAndStudentId(999L, 7L)).isEmpty();
        assertThat(adapter.existsByAssignmentIdAndStudentId(101L, 7L)).isTrue();
        assertThat(adapter.findAssignmentIdsByStudentId(7L)).containsExactlyInAnyOrder(101L, 102L);
        assertThat(adapter.findAssignmentIdsByStudentId(999L)).isEmpty();
        Map<SubmissionStatus, Long> counts = adapter.countGroupedByStatus().stream().collect(Collectors.toMap(
                row -> (SubmissionStatus) row[0], row -> ((Number) row[1]).longValue()));
        assertThat(counts).containsEntry(SubmissionStatus.PENDING, 2L)
                .containsEntry(SubmissionStatus.GRADED, 1L);
        assertThat(adapter.countGroupedByStatusSince(Instant.now().minusSeconds(60))).hasSize(2);
        assertThat(adapter.countGroupedByStatusSince(Instant.now().plusSeconds(60))).isEmpty();
        assertThat(adapter.findById(pending.getId())).contains(pending);
        assertThat(adapter.findById(-1L)).isEmpty();
        entityManager.clear();
        jdbcTemplate.execute("SELECT set_config('app.tenant_id', '12', true)");
        assertThat(adapter.findByAssignmentId(101L, PageRequest.of(0, 10)).getContent()).isEmpty();
        assertThat(adapter.findById(pending.getId())).isEmpty();
        jdbcTemplate.execute("SELECT set_config('app.tenant_id', '', true)");
        assertThat(adapter.findByStudentId(7L, PageRequest.of(0, 10)).getContent()).isEmpty();
        jdbcTemplate.execute("SELECT set_config('app.tenant_id', '11', true)");
        assertThat(adapter.save(AssignmentSubmission.create(103L, 9L)).getId()).isNotNull();
        assertThat(other.getId()).isNotNull();
    }

    @Test void storageFailureIsNotConvertedToAnEmptyResult() {
        var failure = new DataAccessResourceFailureException("database unavailable");
        var failingRepository = mock(AssignmentSubmissionJpaRepository.class);
        when(failingRepository.findById(42L)).thenThrow(failure);

        assertThatThrownBy(() -> new AssignmentSubmissionPersistenceAdapter(failingRepository).findById(42L))
                .isSameAs(failure);
    }
}
