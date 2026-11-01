package com.lumie.lecture.adapter.out.persistence;

import com.lumie.common.test.TenantContextExtension;
import com.lumie.lecture.application.port.out.LectureClassTargetPersistencePort;
import com.lumie.lecture.domain.entity.Lecture;
import com.lumie.lecture.domain.entity.LectureClassTarget;
import com.lumie.lecture.domain.vo.LectureVisibility;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestConstructor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(
        excludeAutoConfiguration = FlywayAutoConfiguration.class,
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect"
        }
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("integration")
@Testcontainers
@ContextConfiguration(classes = LectureClassTargetPersistenceAdapterTest.JpaTestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ExtendWith(TenantContextExtension.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class LectureClassTargetPersistenceAdapterTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    private final LectureClassTargetPersistencePort persistence;
    private final EntityManager entityManager;

    LectureClassTargetPersistenceAdapterTest(
            LectureClassTargetPersistencePort persistence,
            EntityManager entityManager) {
        this.persistence = persistence;
        this.entityManager = entityManager;
    }

    @Test
    void preservesOrderedTargetsCountProjectionAndEmptyResults() {
        Lecture lecture = persistLecture("Boundary lecture");
        persistence.save(LectureClassTarget.create(lecture, 30L));
        persistence.save(LectureClassTarget.create(lecture, 10L));
        persistence.flush();

        assertThat(persistence.findByLectureIdOrderByClassIdAsc(lecture.getId()))
                .extracting(LectureClassTarget::getClassId)
                .containsExactly(10L, 30L);
        assertThat(persistence.findClassIdsByLectureIdOrderByClassIdAsc(lecture.getId()))
                .containsExactly(10L, 30L);
        assertThat(persistence.countByLectureId(lecture.getId())).isEqualTo(2L);
        assertThat(persistence.findRowsByLectureIds(List.of(lecture.getId())))
                .containsExactly(new LectureClassTargetPersistencePort.LectureClassTargetRow(lecture.getId(), 10L),
                        new LectureClassTargetPersistencePort.LectureClassTargetRow(lecture.getId(), 30L));
        assertThat(persistence.findByLectureIdOrderByClassIdAsc(-1L)).isEmpty();
        assertThat(persistence.findClassIdsByLectureIdOrderByClassIdAsc(-1L)).isEmpty();
        assertThat(persistence.countByLectureId(-1L)).isZero();
        assertThat(persistence.findRowsByLectureIds(List.of())).isEmpty();
    }

    @Test
    void deleteByLectureIdRemovesOnlyThatLecturesTargets() {
        Lecture first = persistLecture("First lecture");
        Lecture second = persistLecture("Second lecture");
        persistence.save(LectureClassTarget.create(first, 10L));
        persistence.save(LectureClassTarget.create(second, 20L));
        persistence.flush();

        persistence.deleteByLectureId(first.getId());

        assertThat(persistence.countByLectureId(first.getId())).isZero();
        assertThat(persistence.findClassIdsByLectureIdOrderByClassIdAsc(second.getId()))
                .containsExactly(20L);
    }

    @Test
    void duplicateLectureClassTargetStillPropagatesPersistenceFailure() {
        Lecture lecture = persistLecture("Duplicate target lecture");
        persistence.save(LectureClassTarget.create(lecture, 10L));
        persistence.flush();

        assertThatThrownBy(() -> {
            persistence.save(LectureClassTarget.create(lecture, 10L));
            persistence.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    private Lecture persistLecture(String title) {
        Lecture lecture = Lecture.create(5L, title, null, null, LectureVisibility.STUDENT, null);
        entityManager.persist(lecture);
        entityManager.flush();
        entityManager.clear();
        return entityManager.find(Lecture.class, lecture.getId());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = Lecture.class)
    @EnableJpaRepositories(basePackageClasses = LectureClassTargetJpaRepository.class)
    @EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
    @Import(LectureClassTargetPersistenceAdapter.class)
    static class JpaTestConfiguration {
        @org.springframework.context.annotation.Bean
        org.springframework.data.auditing.DateTimeProvider auditingDateTimeProvider() {
            return () -> Optional.of(Instant.now());
        }
    }
}
