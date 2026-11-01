package com.lumie.ai.adapter.out.persistence;

import com.lumie.ai.domain.entity.ChatMessage;
import com.lumie.ai.domain.vo.MessageRole;
import com.lumie.common.tenant.TenantContextHolder;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@Testcontainers
@DataJpaTest(
        excludeAutoConfiguration = FlywayAutoConfiguration.class,
        properties = {
                "spring.jpa.hibernate.ddl-auto=create",
                "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect"
        }
)
@Import({ChatMessagePersistenceAdapter.class, ChatMessagePersistenceAdapterTest.AuditingConfig.class})
class ChatMessagePersistenceAdapterTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    private final ChatMessageJpaRepository repository;
    private final ChatMessagePersistenceAdapter adapter;
    private final JdbcTemplate jdbcTemplate;
    private final EntityManager entityManager;

    @Autowired
    ChatMessagePersistenceAdapterTest(ChatMessageJpaRepository repository,
                                      ChatMessagePersistenceAdapter adapter,
                                      JdbcTemplate jdbcTemplate,
                                      EntityManager entityManager) {
        this.repository = repository;
        this.adapter = adapter;
        this.jdbcTemplate = jdbcTemplate;
        this.entityManager = entityManager;
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @BeforeEach
    void enforceTenantIsolation() {
        AiRlsTestSupport.bindRestrictedTenant(jdbcTemplate, "ai_chat_messages");
    }

    @Test
    void savesFindsAndListsConversationMessagesInCreatedOrder() {
        TenantContextHolder.setTenant("tenant-a");
        TenantContextHolder.setTenantId(1L);
        ChatMessage later = adapter.save(message(20L, "later"));
        ChatMessage earlier = adapter.save(message(20L, "earlier"));
        repository.flush();
        // Use timestamps that are deterministic even on databases with coarse clock resolution.
        jdbcTemplate.update("UPDATE ai_chat_messages SET created_at = ? WHERE id = ?",
                java.sql.Timestamp.from(java.time.Instant.parse("2026-01-01T00:00:00Z")), earlier.getId());
        jdbcTemplate.update("UPDATE ai_chat_messages SET created_at = ? WHERE id = ?",
                java.sql.Timestamp.from(java.time.Instant.parse("2026-01-01T00:00:01Z")), later.getId());

        assertThat(adapter.findById(earlier.getId())).contains(earlier);
        assertThat(adapter.findByConversationIdOrderByCreatedAtAsc(20L))
                .extracting(ChatMessage::getContent)
                .containsExactly("earlier", "later");
        assertThat(adapter.findByConversationIdOrderByCreatedAtAsc(999L)).isEmpty();
        assertThat(adapter.findById(-1L)).isEmpty();

        entityManager.clear();
        jdbcTemplate.execute("SET LOCAL app.tenant_id = '2'");
        assertThat(adapter.findById(earlier.getId())).isEmpty();
    }

    private static ChatMessage message(Long conversationId, String content) {
        return ChatMessage.builder()
                .conversationId(conversationId)
                .role(MessageRole.USER)
                .content(content)
                .build();
    }

    @EnableJpaAuditing(dateTimeProviderRef = "auditingInstantProvider")
    static class AuditingConfig {
        @org.springframework.context.annotation.Bean
        DateTimeProvider auditingInstantProvider() {
            return () -> Optional.of(Instant.now());
        }
    }
}
