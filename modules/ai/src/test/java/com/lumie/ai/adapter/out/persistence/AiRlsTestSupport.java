package com.lumie.ai.adapter.out.persistence;

import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

final class AiRlsTestSupport {

    private static final String APP_ROLE = "ai_rls_test";

    private AiRlsTestSupport() {
    }

    static void bindRestrictedTenant(JdbcTemplate jdbc, String table) {
        if (!table.equals("ai_chat_messages") && !table.equals("ai_conversations")) {
            throw new IllegalArgumentException("Unexpected AI table: " + table);
        }
        jdbc.execute("ALTER TABLE " + table + " ENABLE ROW LEVEL SECURITY");
        jdbc.execute("ALTER TABLE " + table + " FORCE ROW LEVEL SECURITY");
        jdbc.execute("CREATE POLICY tenant_isolation ON " + table
                + " USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)"
                + " WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)");
        jdbc.execute("CREATE ROLE " + APP_ROLE + " NOSUPERUSER NOBYPASSRLS");
        jdbc.execute("GRANT USAGE ON SCHEMA public TO " + APP_ROLE);
        jdbc.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON " + table + " TO " + APP_ROLE);
        jdbc.execute("GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO " + APP_ROLE);
        jdbc.execute("SET LOCAL ROLE " + APP_ROLE);
        jdbc.execute("SET LOCAL app.tenant_id = '1'");
        assertThat(jdbc.queryForObject("SELECT current_user", String.class)).isEqualTo(APP_ROLE);
    }
}
