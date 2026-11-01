package com.lumie.assignment.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.lumie.assignment.domain.entity.AssignmentRecipient;
import com.lumie.common.tenant.TenantContextHolder;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestConstructor;
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
@Import(AssignmentRecipientPersistenceAdapter.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AssignmentRecipientPersistenceAdapterTest {
    @Container static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine").withInitScript("assignment-test-runtime-role.sql");
    @DynamicPropertySource static void dataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", () -> "lumie_app");
        registry.add("spring.datasource.password", () -> "lumie_app");
    }

    private final AssignmentRecipientJpaRepository repository;
    private final AssignmentRecipientPersistenceAdapter adapter;
    private final JdbcTemplate jdbcTemplate;

    AssignmentRecipientPersistenceAdapterTest(AssignmentRecipientJpaRepository repository,
            AssignmentRecipientPersistenceAdapter adapter, JdbcTemplate jdbcTemplate) {
        this.repository = repository;
        this.adapter = adapter;
        this.jdbcTemplate = jdbcTemplate;
    }

    @AfterEach void clearTenant() { TenantContextHolder.clear(); }

    @Test void allRecipientQueriesAndBulkSavePreserveResultsAndEmptyCases() {
        TenantContextHolder.setTenantId(11L);
        AssignmentPersistenceTestApplication.enableTenantRls(jdbcTemplate, "assignment_recipients");
        var one = repository.save(AssignmentRecipient.create(101L, 7L));
        var two = repository.save(AssignmentRecipient.create(102L, 7L));
        repository.save(AssignmentRecipient.create(101L, 8L));
        repository.flush();

        assertThat(adapter.findByAssignmentId(101L)).extracting(AssignmentRecipient::getStudentId).containsExactlyInAnyOrder(7L, 8L);
        assertThat(adapter.findByAssignmentId(999L)).isEmpty();
        assertThat(adapter.findByAssignmentIdIn(List.of(101L, 102L))).hasSize(3);
        assertThat(adapter.findByAssignmentIdIn(List.of())).isEmpty();
        assertThat(adapter.existsByAssignmentIdAndSourceProvenance(101L, "EXACT")).isTrue();
        assertThat(adapter.existsByAssignmentIdAndStudentId(101L, 7L)).isTrue();
        assertThat(adapter.findByAssignmentIdAndStudentId(101L, 7L)).contains(one);
        assertThat(adapter.findByAssignmentIdAndStudentId(101L, 9L)).isEmpty();
        assertThat(adapter.findAssignmentIdsByStudentId(7L)).containsExactlyInAnyOrder(101L, 102L);
        assertThat(adapter.findStudentIdsByAssignmentId(101L)).containsExactlyInAnyOrder(7L, 8L);
        assertThat(adapter.findAssignmentIdsByStudentIdIn(List.of(7L, 8L))).contains(101L, 102L);
        assertThat(adapter.findAssignmentIdsByStudentIdIn(List.of())).isEmpty();

        jdbcTemplate.execute("SELECT set_config('app.tenant_id', '12', true)");
        assertThat(adapter.findByAssignmentId(101L)).isEmpty();
        assertThat(adapter.existsByAssignmentIdAndStudentId(101L, 7L)).isFalse();
        jdbcTemplate.execute("SELECT set_config('app.tenant_id', '', true)");
        assertThat(adapter.findByAssignmentId(101L)).isEmpty();
        jdbcTemplate.execute("SELECT set_config('app.tenant_id', '11', true)");

        var saved = adapter.saveAll(List.of(AssignmentRecipient.create(103L, 9L)));
        assertThat(saved).hasSize(1);
        assertThat(saved.getFirst().getId()).isNotNull();
        assertThat(two.getId()).isNotNull();
    }

    @Test void storageFailureIsNotConvertedToAnEmptyResult() {
        var failure = new DataAccessResourceFailureException("database unavailable");
        var failingRepository = mock(AssignmentRecipientJpaRepository.class);
        when(failingRepository.findByAssignmentId(101L)).thenThrow(failure);

        assertThatThrownBy(() -> new AssignmentRecipientPersistenceAdapter(failingRepository)
                .findByAssignmentId(101L)).isSameAs(failure);
    }
}
