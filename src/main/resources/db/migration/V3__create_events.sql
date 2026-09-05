CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    total_tickets INT NOT NULL CHECK (total_tickets >= 0),
    tickets_sold INT NOT NULL DEFAULT 0 CHECK (tickets_sold >= 0 AND tickets_sold <= total_tickets),
    sale_start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    waiting_room_open_offset_seconds INT NOT NULL DEFAULT 900 CHECK (waiting_room_open_offset_seconds >= 0),
    status VARCHAR(32) NOT NULL DEFAULT 'UPCOMING',
    queue_finalized_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_events_status ON events(status);
CREATE INDEX idx_events_sale_start ON events(sale_start_time);
