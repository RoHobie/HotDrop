-- Seed initial demo flash-sale events for quick exploration
INSERT INTO events (name, description, total_tickets, tickets_sold, sale_start_time, waiting_room_open_offset_seconds, status, created_at)
SELECT
    'Cyberpunk 2099 World Tour',
    'Exclusive arena concert flash drop with immersive neural audio and holographic stages.',
    250,
    0,
    CURRENT_TIMESTAMP + INTERVAL '1' DAY,
    900,
    'UPCOMING',
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM events WHERE name = 'Cyberpunk 2099 World Tour');

INSERT INTO events (name, description, total_tickets, tickets_sold, sale_start_time, waiting_room_open_offset_seconds, status, created_at)
SELECT
    'Neon Genesis Live - Exclusive Drop',
    'Limited admission festival pass with fair queue randomization and instant checkout.',
    50,
    0,
    CURRENT_TIMESTAMP - INTERVAL '1' HOUR,
    900,
    'LIVE',
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM events WHERE name = 'Neon Genesis Live - Exclusive Drop');
