CREATE TABLE queue_entries (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    queue_position BIGINT,
    status VARCHAR(32) NOT NULL DEFAULT 'WAITING',
    admitted_at TIMESTAMP WITH TIME ZONE,
    admission_expires_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_queue_event_user UNIQUE (event_id, user_id)
);

CREATE INDEX idx_queue_entries_event_status ON queue_entries(event_id, status);
CREATE INDEX idx_queue_entries_pos ON queue_entries(event_id, queue_position);
