package com.lumie.lecture.adapter.out.persistence;

import com.lumie.common.test.TenantContextExtension;
import com.lumie.lecture.application.port.out.LectureClassTargetPersistencePort;
import com.lumie.lecture.application.port.out.LecturePersistencePort;
import com.lumie.lecture.domain.entity.Lecture;
import com.lumie.lecture.domain.entity.LectureClassTarget;
import com.lumie.lecture.domain.vo.LectureSearchCriteria;
import com.lumie.lecture.domain.vo.LectureVisibility;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
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
@ContextConfiguration(classes = LecturePersistenceAdapterTest.JpaTestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ExtendWith(TenantContextExtension.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class LecturePersistenceAdapterTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    private final LecturePersistencePort lectures;
    private final LectureClassTargetPersistencePort targets;
    private final EntityManager entityManager;

    LecturePersistenceAdapterTest(
            LecturePersistencePort lectures,
            LectureClassTargetPersistencePort targets,
            EntityManager entityManager) {
        this.lectures = lectures;
        this.targets = targets;
        this.entityManager = entityManager;
    }

    @Test
    void visibleByTargetClassFiltersVisibilityAndPreservesPageMetadata() {
        Lecture visible = persistLecture("Visible", 7L, LectureVisibility.STUDENT,
                LocalDate.of(2025, 1, 10), Instant.parse("2025-01-01T00:00:00Z"));
        Lecture hidden = persistLecture("Hidden", 7L, LectureVisibility.TEACHER_ONLY,
                LocalDate.of(2025, 1, 11), Instant.parse("2025-01-02T00:00:00Z"));
        Lecture otherClass = persistLecture("Other class", 8L, LectureVisibility.STUDENT,
                LocalDate.of(2025, 1, 12), Instant.parse("2025-01-03T00:00:00Z"));
        targets.save(LectureClassTarget.create(visible, 11L));
        targets.save(LectureClassTarget.create(hidden, 11L));
        targets.save(LectureClassTarget.create(otherClass, 12L));
        targets.flush();

        var result = lectures.findVisibleByTargetClassId(11L, PageRequest.of(0, 1));

        assertThat(result.getContent()).extracting(Lecture::getId).containsExactly(visible.getId());
        assertThat(result.getTotalElements()).isEqualTo(1L);
        assertThat(result.getNumber()).isZero();
        assertThat(lectures.findVisibleByTargetClassId(99L, PageRequest.of(0, 10)).getContent()).isEmpty();
    }

    @Test
    void criteriaSearchPreservesClassTeacherDateAndCaseInsensitiveTextFilters() {
        Lecture expected = persistLecture("Science review", 7L, LectureVisibility.STUDENT,
                LocalDate.of(2025, 3, 15), Instant.parse("2025-03-01T00:00:00Z"));
        Lecture wrongDate = persistLecture("Science earlier", 7L, LectureVisibility.STUDENT,
                LocalDate.of(2025, 1, 15), Instant.parse("2025-02-01T00:00:00Z"));
        Lecture wrongTeacher = persistLecture("Science other teacher", 8L, LectureVisibility.STUDENT,
                LocalDate.of(2025, 3, 16), Instant.parse("2025-04-01T00:00:00Z"));
        Lecture wrongClass = persistLecture("Science other class", 7L, LectureVisibility.STUDENT,
                LocalDate.of(2025, 3, 17), Instant.parse("2025-05-01T00:00:00Z"));
        targets.save(LectureClassTarget.create(expected, 11L));
        targets.save(LectureClassTarget.create(wrongDate, 11L));
        targets.save(LectureClassTarget.create(wrongTeacher, 11L));
        targets.save(LectureClassTarget.create(wrongClass, 12L));
        targets.flush();

        LectureSearchCriteria criteria = new LectureSearchCriteria(
                11L,
                7L,
                LocalDate.of(2025, 2, 1),
                LocalDate.of(2025, 3, 31),
                "sCiEnCe"
        );

        assertThat(lectures.findAll(criteria, PageRequest.of(0, 10)).getContent())
                .extracting(Lecture::getId)
                .containsExactly(expected.getId());
        assertThat(lectures.findAll(criteria, PageRequest.of(1, 10)).getContent()).isEmpty();
    }

    @Test
    void criteriaSearchPreservesDefaultAndCallerProvidedSort() {
        Lecture older = persistLecture("Zulu", 7L, LectureVisibility.STUDENT,
                LocalDate.of(2025, 1, 1), Instant.parse("2025-01-01T00:00:00Z"));
        Lecture newer = persistLecture("Alpha", 7L, LectureVisibility.STUDENT,
                LocalDate.of(2025, 1, 2), Instant.parse("2025-01-02T00:00:00Z"));
        LectureSearchCriteria criteria = new LectureSearchCriteria(null, null, null, null, null);

        assertThat(lectures.findAll(criteria, PageRequest.of(0, 10)).getContent())
                .extracting(Lecture::getId)
                .containsExactly(newer.getId(), older.getId());
        assertThat(lectures.findAll(criteria, PageRequest.of(0, 10, Sort.by("title"))).getContent())
                .extracting(Lecture::getId)
                .containsExactly(newer.getId(), older.getId());
    }

    @Test
    void inheritedCrudOperationsSaveFindAndDeleteWithoutHidingMissingRows() {
        Lecture saved = lectures.save(Lecture.create(7L, "CRUD", null, null, LectureVisibility.TEACHER_ONLY, null));
        entityManager.flush();

        assertThat(lectures.findById(saved.getId())).isPresent();
        assertThat(lectures.findById(-1L)).isEmpty();

        lectures.delete(saved);
        entityManager.flush();

        assertThat(lectures.findById(saved.getId())).isEmpty();
    }

    @Test
    void invalidLectureStillPropagatesPersistenceFailure() {
        Lecture invalid = Lecture.create(7L, null, null, null, LectureVisibility.TEACHER_ONLY, null);

        assertThatThrownBy(() -> {
            lectures.save(invalid);
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    private Lecture persistLecture(String title, Long teacherId, LectureVisibility visibility,
                                   LocalDate lectureDate, Instant createdAt) {
        Lecture lecture = Lecture.create(teacherId, title, title + " description", null, visibility, lectureDate);
        ReflectionTestUtils.setField(lecture, "createdAt", createdAt);
        ReflectionTestUtils.setField(lecture, "updatedAt", createdAt);
        entityManager.persist(lecture);
        entityManager.flush();
        entityManager.clear();
        return entityManager.find(Lecture.class, lecture.getId());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = Lecture.class)
    @EnableJpaRepositories(basePackageClasses = LectureJpaRepository.class)
    @EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
    @Import({LecturePersistenceAdapter.class, LectureClassTargetPersistenceAdapter.class})
    static class JpaTestConfiguration {
        @Bean
        org.springframework.data.auditing.DateTimeProvider auditingDateTimeProvider() {
            return () -> Optional.of(Instant.now());
        }
    }
}
