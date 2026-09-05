-- Performance optimization indexes for hot query paths
CREATE INDEX IF NOT EXISTS idx_events_status_start ON events(status, sale_start_time);
CREATE INDEX IF NOT EXISTS idx_queue_entries_event_status_pos ON queue_entries(event_id, status, queue_position);
CREATE INDEX IF NOT EXISTS idx_queue_entries_admission_exp ON queue_entries(status, admission_expires_at);
CREATE INDEX IF NOT EXISTS idx_bookings_event_status ON bookings(event_id, status);
