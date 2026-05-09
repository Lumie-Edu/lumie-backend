CREATE TABLE activity_logs (
    id            BIGSERIAL PRIMARY KEY,
    actor_id      BIGINT       NOT NULL,
    actor_name    VARCHAR(100) NOT NULL,
    actor_role    VARCHAR(20)  NOT NULL,
    action        VARCHAR(30)  NOT NULL,
    entity_type   VARCHAR(50)  NOT NULL,
    entity_id     BIGINT,
    entity_name   VARCHAR(200),
    description   VARCHAR(500),
    created_at    TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_activity_logs_actor_id ON activity_logs (actor_id);
CREATE INDEX idx_activity_logs_actor_role ON activity_logs (actor_role);
CREATE INDEX idx_activity_logs_created_at ON activity_logs (created_at DESC);
CREATE INDEX idx_activity_logs_entity_type ON activity_logs (entity_type);
