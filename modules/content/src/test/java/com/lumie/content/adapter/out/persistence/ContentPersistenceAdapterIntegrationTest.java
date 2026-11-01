package com.lumie.content.adapter.out.persistence;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.content.application.port.out.*;
import com.lumie.content.domain.entity.*;
import com.lumie.content.domain.vo.AnnouncementAudienceScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.sql.Timestamp;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@Testcontainers
@DataJpaTest(
        excludeAutoConfiguration = FlywayAutoConfiguration.class,
        properties = {
                "spring.jpa.hibernate.ddl-auto=create",
                "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect"
        })
@Import({
        AnnouncementClassTargetPersistenceAdapter.class,
        AnnouncementReadPersistenceAdapter.class,
        AnnouncementPersistenceAdapter.class,
        QnaBoardPersistenceAdapter.class,
        QnaCommentPersistenceAdapter.class,
        ReviewPopupSettingPersistenceAdapter.class,
        ReviewPersistenceAdapter.class
})
class ContentPersistenceAdapterIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine");

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired AnnouncementClassTargetPersistencePort classTargets;
    @Autowired AnnouncementReadPersistencePort announcementReads;
    @Autowired AnnouncementPersistencePort announcements;
    @Autowired QnaBoardPersistencePort qnaBoards;
    @Autowired QnaCommentPersistencePort qnaComments;
    @Autowired ReviewPopupSettingPersistencePort popupSettings;
    @Autowired ReviewPersistencePort reviews;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void enableTenantPolicies() {
        TenantContextHolder.setTenant("test");
        TenantContextHolder.setTenantId(1L);
        jdbc.execute("CREATE ROLE lumie_content_test NOLOGIN");
        for (String table : List.of(
                "announcements", "announcement_class_targets", "announcement_reads", "qna_boards",
                "qna_comments", "reviews", "toggles")) {
            jdbc.execute("ALTER TABLE " + table + " ENABLE ROW LEVEL SECURITY");
            jdbc.execute("ALTER TABLE " + table + " FORCE ROW LEVEL SECURITY");
            jdbc.execute("CREATE POLICY test_tenant_isolation ON " + table + " USING (tenant_id = "
                    + "NULLIF(current_setting('app.tenant_id', true), '')::bigint) WITH CHECK (tenant_id = "
                    + "NULLIF(current_setting('app.tenant_id', true), '')::bigint)");
        }
        jdbc.execute("GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO lumie_content_test");
        jdbc.execute("GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO lumie_content_test");
        jdbc.execute("SET LOCAL ROLE lumie_content_test");
        bindTenant(1L);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("ATDD-US1-001:example-004 content announcement lookup")
    void announcementQueriesPreserveAudienceFiltersOrderingAndTenantIsolation() {
        Announcement global = persistAnnouncement("global", true, AnnouncementAudienceScope.ALL);
        Announcement targeted = persistAnnouncement("targeted", false, AnnouncementAudienceScope.TARGETED);
        Announcement foreign = persistAnnouncement("foreign", true, AnnouncementAudienceScope.ALL);
        bindTenant(2L);
        var otherTenant = announcements.save(Announcement.create(
                3L, "other tenant", "body", true, true, AnnouncementAudienceScope.ALL));
        bindTenant(1L);
        AnnouncementClassTarget target = AnnouncementClassTarget.create(targeted.getId(), 77L);
        classTargets.saveAll(List.of(target));
        classTargets.flush();

        var page = PageRequest.of(0, 10, Sort.unsorted());
        assertThat(announcements.findAcademyWide(page).getContent()).extracting(Announcement::getId)
                .contains(global.getId(), foreign.getId()).doesNotContain(targeted.getId(), otherTenant.getId());
        assertThat(announcements.findVisibleToClasses(List.of(77L), page).getContent())
                .extracting(Announcement::getId).contains(targeted.getId(), global.getId());
        assertThat(announcements.findAcademyWideByIsAsset(true, page).getContent())
                .extracting(Announcement::getId).contains(global.getId());
        assertThat(announcements.findVisibleToClassesByIsAsset(false, List.of(77L), page).getContent())
                .extracting(Announcement::getId).containsExactly(targeted.getId());
        assertThat(announcements.findImportantAcademyWide()).extracting(Announcement::getId)
                .contains(global.getId());
        assertThat(announcements.findImportantVisibleToClasses(List.of(77L)))
                .extracting(Announcement::getId).contains(global.getId(), foreign.getId());
        assertThat(announcements.findByIsItImportantAnnouncementTrue()).extracting(Announcement::getId)
                .contains(global.getId(), foreign.getId());
        assertThat(announcements.findAllOrderByImportantDescCreatedAtDesc(page).getTotalElements()).isEqualTo(3);
        assertThat(announcements.findByIsItAssetAnnouncementOrderByImportantDescCreatedAtDesc(true, page)
                .getTotalElements()).isEqualTo(2);
        announcements.deleteById(foreign.getId());
        announcements.flush();
        assertThat(announcements.existsById(foreign.getId())).isFalse();
        assertThat(announcements.findById(global.getId())).contains(global);
    }

    @Test
    void classTargetOperationsPreserveLookupBatchingAndDeleteBehavior() {
        Announcement announcement = persistAnnouncement("target owner", false, AnnouncementAudienceScope.TARGETED);
        Announcement otherAnnouncement = persistAnnouncement("other owner", false, AnnouncementAudienceScope.TARGETED);
        classTargets.saveAll(List.of(
                AnnouncementClassTarget.create(announcement.getId(), 77L),
                AnnouncementClassTarget.create(otherAnnouncement.getId(), 88L)));
        classTargets.flush();

        assertThat(classTargets.findByAnnouncementId(announcement.getId())).extracting(
                AnnouncementClassTarget::getClassId).containsExactly(77L);
        assertThat(classTargets.findByAnnouncementIdIn(List.of(announcement.getId(), otherAnnouncement.getId())))
                .hasSize(2);
        classTargets.deleteByAnnouncementId(announcement.getId());
        classTargets.flush();
        assertThat(classTargets.findByAnnouncementId(announcement.getId())).isEmpty();
    }

    @Test
    void announcementReadInsertIsIdempotentOrderedAndTenantScoped() {
        Announcement announcement = persistAnnouncement("read target", false, AnnouncementAudienceScope.ALL);
        assertThat(announcementReads.insertIfAbsent(1L, announcement.getId(), 10L)).isEqualTo(1);
        assertThat(announcementReads.insertIfAbsent(1L, announcement.getId(), 10L)).isZero();
        assertThat(announcementReads.insertIfAbsent(1L, announcement.getId(), 11L)).isEqualTo(1);
        jdbc.update("UPDATE announcement_reads SET read_at = ? WHERE user_id = ?",
                Timestamp.from(Instant.parse("2026-01-01T00:00:00Z")), 10L);
        jdbc.update("UPDATE announcement_reads SET read_at = ? WHERE user_id = ?",
                Timestamp.from(Instant.parse("2026-01-02T00:00:00Z")), 11L);
        assertThat(announcementReads.findUserIdsByAnnouncementId(announcement.getId())).containsExactly(11L, 10L);
        bindTenant(2L);
        assertThat(announcementReads.findUserIdsByAnnouncementId(announcement.getId())).isEmpty();
    }

    @Test
    @DisplayName("ATDD-US1-001:example-005 content Q&A lookup")
    void qnaBoardQueriesPreserveSearchFetchAndAbsenceBehavior() {
        QnaBoard alpha = qnaBoards.save(QnaBoard.create(101L, "Alpha question", "Need help"));
        QnaBoard beta = qnaBoards.save(QnaBoard.create(102L, "Beta", "alpha details"));
        QnaBoard foreign = qnaBoards.save(QnaBoard.create(101L, "Alpha foreign", "other"));
        QnaComment comment = qnaComments.save(QnaComment.createByStaff(alpha, 900L, "answer"));
        alpha.addComment(comment);
        alpha.markAsAnswered();
        qnaBoards.save(alpha);
        qnaBoards.flush();

        var page = PageRequest.of(0, 10);
        assertThat(qnaBoards.findByIdWithComments(alpha.getId()).orElseThrow().getComments()).hasSize(1);
        assertThat(qnaBoards.findByQnaUserId(101L, page).getContent()).contains(alpha, foreign);
        assertThat(qnaBoards.findByIsAnsweredFalse(page).getContent()).contains(beta);
        assertThat(qnaBoards.findByIsAnswered(true, page).getContent()).contains(alpha);
        assertThat(qnaBoards.countByIsAnsweredFalseAndCreatedAtBefore(Instant.now().plusSeconds(1))).isEqualTo(2);
        assertThat(qnaBoards.search("alpha", List.of(102L), null, page).getContent()).contains(alpha, beta);
        assertThat(qnaBoards.searchByUser(101L, "alpha", null, page).getContent()).contains(alpha, foreign);
        assertThat(qnaBoards.findById(-1L)).isEmpty();
    }

    @Test
    void qnaCommentPersistencePreservesLookupAndDeletion() {
        QnaBoard board = qnaBoards.save(QnaBoard.create(101L, "Question", "Body"));
        QnaComment comment = qnaComments.save(QnaComment.createByStaff(board, 900L, "answer"));
        board.addComment(comment);
        qnaBoards.save(board);
        qnaBoards.flush();

        assertThat(qnaComments.findByQnaBoardId(board.getId())).hasSize(1);
        qnaComments.delete(comment);
        board.getComments().remove(comment);
        qnaBoards.flush();
        assertThat(qnaComments.findByQnaBoardId(board.getId())).isEmpty();
    }

    @Test
    void reviewPersistencePreservesCrudAndPaging() {
        var page = PageRequest.of(0, 10);
        Review review = reviews.save(Review.create("Reviewer", "Title", "Content"));
        assertThat(reviews.findAll(page).getContent()).containsExactly(review);
        assertThat(reviews.existsById(review.getId())).isTrue();
        reviews.deleteById(review.getId());
        assertThat(reviews.existsById(review.getId())).isFalse();
    }

    @Test
    void popupSettingPersistencePreservesFirstAndSaveBehavior() {
        assertThat(popupSettings.findFirst()).isEmpty();
        ReviewPopupSetting setting = popupSettings.save(new ReviewPopupSetting(1L, true));
        assertThat(popupSettings.findFirst()).contains(setting);
    }

    private Announcement persistAnnouncement(String title, boolean important, AnnouncementAudienceScope scope) {
        boolean asset = title.equals("global") || title.equals("foreign");
        return announcements.save(Announcement.create(5L, title, "body", asset, important, scope));
    }

    private void bindTenant(long tenantId) {
        TenantContextHolder.setTenantId(tenantId);
        jdbc.queryForObject("SELECT set_config('app.tenant_id', ?, true)", String.class, Long.toString(tenantId));
    }
}
