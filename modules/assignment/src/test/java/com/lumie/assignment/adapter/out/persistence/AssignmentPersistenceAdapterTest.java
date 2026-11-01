package com.lumie.assignment.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.lumie.assignment.domain.entity.Assignment;
import com.lumie.assignment.domain.entity.AssignmentClassTarget;
import com.lumie.assignment.domain.vo.AssignmentSearchCriteria;
import com.lumie.assignment.domain.vo.AssignmentStatus;
import com.lumie.common.tenant.TenantContextHolder;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
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
@Import(AssignmentPersistenceAdapter.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AssignmentPersistenceAdapterTest {
    @Container static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine").withInitScript("assignment-test-runtime-role.sql");
    @DynamicPropertySource static void dataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", () -> "lumie_app");
        registry.add("spring.datasource.password", () -> "lumie_app");
    }

    private final AssignmentJpaRepository repository;
    private final AssignmentPersistenceAdapter adapter;
    private final AssignmentClassTargetJpaRepository targetRepository;
    private final JdbcTemplate jdbcTemplate;
    private final EntityManager entityManager;

    AssignmentPersistenceAdapterTest(AssignmentJpaRepository repository, AssignmentPersistenceAdapter adapter,
            AssignmentClassTargetJpaRepository targetRepository, JdbcTemplate jdbcTemplate,
            EntityManager entityManager) {
        this.repository = repository;
        this.adapter = adapter;
        this.targetRepository = targetRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.entityManager = entityManager;
    }

    @AfterEach void clearTenant() { TenantContextHolder.clear(); }

    @Test void assignmentQueriesAndOverdueUpdatesKeepFiltersAndMutationCounts() {
        TenantContextHolder.setTenantId(11L);
        AssignmentPersistenceTestApplication.enableTenantRls(jdbcTemplate, "assignments");
        AssignmentPersistenceTestApplication.enableTenantRls(jdbcTemplate, "assignment_class_targets");
        Instant now = Instant.parse("2026-10-29T00:00:00Z");
        Assignment active = repository.save(Assignment.create(now.plusSeconds(60), 100, List.of()));
        Assignment overdue = repository.save(Assignment.create(now.minusSeconds(60), 100, List.of()));
        Assignment overdueBulk = repository.save(Assignment.create(now.minusSeconds(120), 100, List.of()));
        targetRepository.save(AssignmentClassTarget.create(active.getId(), 55L));
        targetRepository.save(AssignmentClassTarget.create(overdue.getId(), 55L));
        targetRepository.save(AssignmentClassTarget.create(active.getId(), 56L));
        repository.flush();

        assertThat(adapter.findByTargetClassId(55L, PageRequest.of(0, 1)).getTotalElements()).isEqualTo(2);
        assertThat(adapter.findByTargetClassId(999L, PageRequest.of(0, 10)).getContent()).isEmpty();
        assertThat(adapter.findByIdIn(List.of(active.getId(), overdue.getId()))).hasSize(2);
        assertThat(adapter.findByIdIn(List.of())).isEmpty();
        assertThat(adapter.findActiveByTargetClassId(55L, now)).extracting(Assignment::getId).containsExactly(active.getId());
        assertThat(adapter.findAll(new AssignmentSearchCriteria(55L, AssignmentStatus.ACTIVE, now), PageRequest.of(0, 10))
                .getContent()).extracting(Assignment::getId).containsExactly(active.getId());
        assertThat(adapter.findAll(new AssignmentSearchCriteria(55L, AssignmentStatus.CLOSED, now), PageRequest.of(0, 10))
                .getContent()).extracting(Assignment::getId).containsExactly(overdue.getId());
        assertThat(adapter.findAll(new AssignmentSearchCriteria(null, null, now), PageRequest.of(0, 10))
                .getTotalElements()).isEqualTo(3);
        assertThat(adapter.findById(active.getId())).contains(active);

        entityManager.clear();
        jdbcTemplate.execute("SELECT set_config('app.tenant_id', '12', true)");
        assertThat(adapter.findById(active.getId())).isEmpty();
        assertThat(adapter.findByTargetClassId(55L, PageRequest.of(0, 10)).getContent()).isEmpty();
        jdbcTemplate.execute("SELECT set_config('app.tenant_id', '', true)");
        assertThat(adapter.findAll(new AssignmentSearchCriteria(null, null, now), PageRequest.of(0, 10)).getContent()).isEmpty();
        jdbcTemplate.execute("SELECT set_config('app.tenant_id', '11', true)");

        assertThat(adapter.closeOverdueActiveAssignment(active.getId(), now)).isZero();
        assertThat(adapter.closeOverdueActiveAssignment(overdue.getId(), now)).isEqualTo(1);
        assertThat(repository.findById(overdue.getId()).orElseThrow().getStatus()).isEqualTo(AssignmentStatus.CLOSED);
        assertThat(adapter.closeOverdueActiveAssignments(now)).isEqualTo(1);
        assertThat(repository.findById(overdueBulk.getId()).orElseThrow().getStatus()).isEqualTo(AssignmentStatus.CLOSED);
        assertThat(adapter.save(active).getId()).isEqualTo(active.getId());
        adapter.flush();
        adapter.delete(active);
        assertThat(adapter.findById(active.getId())).isEmpty();
    }

    @Test void storageFailureIsNotConvertedToAnEmptyResult() {
        var failure = new DataAccessResourceFailureException("database unavailable");
        var failingRepository = mock(AssignmentJpaRepository.class);
        when(failingRepository.findById(42L)).thenThrow(failure);

        assertThatThrownBy(() -> new AssignmentPersistenceAdapter(failingRepository).findById(42L))
                .isSameAs(failure);
    }
}
