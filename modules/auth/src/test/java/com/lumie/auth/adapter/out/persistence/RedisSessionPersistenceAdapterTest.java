package com.lumie.auth.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lumie.auth.domain.vo.DeviceCategory;
import com.lumie.auth.domain.vo.AuthSession;
import com.lumie.common.auth.Role;
import java.time.Instant;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

class RedisSessionPersistenceAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void unknownRoleDoesNotInvalidateOtherwiseValidSession() throws Exception {
        AuthSession session = RedisSessionPersistenceAdapter.readSession(objectMapper, """
                {
                  "id": "session-id",
                  "userId": 42,
                  "userName": "User",
                  "tenantSlug": "academy",
                  "tenantId": 7,
                  "role": "UNKNOWN_ROLE",
                  "device": "test",
                  "deviceCategory": "PC",
                  "createdAt": "2026-10-20T00:00:00Z",
                  "expiresAt": "2026-10-20T01:00:00Z"
                }
                """);

        assertThat(session.id()).isEqualTo("session-id");
        assertThat(session.role()).isNull();
    }

    @Test
    void findByIdReadsAndDeserializesTheStoredSession() throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        given(redisTemplate.opsForValue()).willReturn(values);
        AuthSession stored = session(Role.OWNER);
        given(values.get("auth:session:session-id")).willReturn(objectMapper.writeValueAsString(stored));
        RedisSessionPersistenceAdapter adapter = new RedisSessionPersistenceAdapter(redisTemplate, objectMapper);

        Optional<AuthSession> result = adapter.findById("session-id");

        assertThat(result).contains(stored);
    }

    @Test
    void findByIdDeletesAnUnreadableSession() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        given(redisTemplate.opsForValue()).willReturn(values);
        given(values.get("auth:session:session-id")).willReturn("not-json");
        RedisSessionPersistenceAdapter adapter = new RedisSessionPersistenceAdapter(redisTemplate, objectMapper);

        assertThat(adapter.findById("session-id")).isEmpty();

        verify(redisTemplate).delete("auth:session:session-id");
    }

    @Test
    void findByIdReturnsEmptyWhenNoRedisValueExists() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        given(redisTemplate.opsForValue()).willReturn(values);
        given(values.get(anyString())).willReturn(null);
        RedisSessionPersistenceAdapter adapter = new RedisSessionPersistenceAdapter(redisTemplate, objectMapper);

        assertThat(adapter.findById("missing")).isEmpty();
    }

    @Test
    void savePersistsSessionAndAddsItsTenantUserIndex() throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        @SuppressWarnings("unchecked")
        SetOperations<String, String> sets = mock(SetOperations.class);
        given(redisTemplate.opsForValue()).willReturn(values);
        given(redisTemplate.opsForSet()).willReturn(sets);
        given(sets.add(anyString(), anyString())).willReturn(1L);
        given(redisTemplate.expire(anyString(), any(Duration.class))).willReturn(true);
        RedisSessionPersistenceAdapter adapter = new RedisSessionPersistenceAdapter(redisTemplate, objectMapper);

        adapter.save(session(Role.STUDENT));

        verify(values).set(eq("auth:session:session-id"), anyString(), any(Duration.class));
        verify(sets).add("auth:user-sessions:academy:42", "session-id");
        verify(redisTemplate).expire(eq("auth:user-sessions:academy:42"), any(Duration.class));
    }

    @Test
    void deleteRemovesSessionAndTenantUserIndexEntry() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        SetOperations<String, String> sets = mock(SetOperations.class);
        given(redisTemplate.opsForSet()).willReturn(sets);
        RedisSessionPersistenceAdapter adapter = new RedisSessionPersistenceAdapter(redisTemplate, objectMapper);
        AuthSession session = session(Role.STUDENT);

        adapter.delete(session);

        verify(redisTemplate).delete("auth:session:session-id");
        verify(sets).remove("auth:user-sessions:academy:42", "session-id");
    }

    @Test
    void deleteAllForUserRemovesIndexedSessionsAndIndex() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        SetOperations<String, String> sets = mock(SetOperations.class);
        given(redisTemplate.opsForSet()).willReturn(sets);
        given(sets.members("auth:user-sessions:academy:42"))
                .willReturn(new LinkedHashSet<>(List.of("first", "second")));
        RedisSessionPersistenceAdapter adapter = new RedisSessionPersistenceAdapter(redisTemplate, objectMapper);

        adapter.deleteAllForUser("academy", 42L);

        verify(redisTemplate).delete(List.of("auth:session:first", "auth:session:second"));
        verify(redisTemplate).delete("auth:user-sessions:academy:42");
    }

    @Test
    void findSessionIdsReturnsEmptyForMissingIndexAndExistingIdsOtherwise() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        SetOperations<String, String> sets = mock(SetOperations.class);
        given(redisTemplate.opsForSet()).willReturn(sets);
        given(sets.members("auth:user-sessions:academy:42"))
                .willReturn(null)
                .willReturn(Set.of("first", "second"));
        RedisSessionPersistenceAdapter adapter = new RedisSessionPersistenceAdapter(redisTemplate, objectMapper);

        assertThat(adapter.findSessionIds("academy", 42L)).isEmpty();
        assertThat(adapter.findSessionIds("academy", 42L)).containsExactlyInAnyOrder("first", "second");
    }

    @Test
    void findSessionsFiltersForeignTenantAndCleansMissingOrUnreadableEntries() throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        @SuppressWarnings("unchecked")
        SetOperations<String, String> sets = mock(SetOperations.class);
        given(redisTemplate.opsForValue()).willReturn(values);
        given(redisTemplate.opsForSet()).willReturn(sets);
        String valid = objectMapper.writeValueAsString(session("academy", Role.STUDENT));
        String foreign = objectMapper.writeValueAsString(session("other-academy", Role.OWNER));
        List<String> ids = Arrays.asList(valid, foreign, null, "not-json");
        given(values.multiGet(any())).willReturn(ids);
        RedisSessionPersistenceAdapter adapter = new RedisSessionPersistenceAdapter(redisTemplate, objectMapper);
        LinkedHashSet<String> requestedIds = new LinkedHashSet<>(List.of("one", "two", "missing", "broken"));

        Map<String, AuthSession> result = adapter.findSessions("academy", 42L, requestedIds);

        assertThat(result).containsOnlyKeys("one");
        assertThat(result.get("one").role()).isEqualTo(Role.STUDENT);
        verify(sets).remove("auth:user-sessions:academy:42", "missing");
        verify(redisTemplate).delete("auth:session:broken");
    }

    private static AuthSession session(Role role) {
        return session("academy", role);
    }

    private static AuthSession session(String tenantSlug, Role role) {
        Instant now = Instant.now();
        return new AuthSession("session-id", 42L, "User", tenantSlug, 7L, role,
                "Chrome", DeviceCategory.PC, now, now.plusSeconds(3600));
    }
}
