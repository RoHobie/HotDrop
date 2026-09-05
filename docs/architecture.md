# HotDrop Architecture Reference

This document captures the architectural decisions, domain models, lifecycle state machines, and concurrency safety guarantees powering HotDrop.

---

## 1. Domain Entities & Database Schema

### Core Tables
1. **`users`**
   - Stores accounts, credentials, and roles.
   - Fields: `id (BIGSERIAL PK)`, `name (VARCHAR)`, `email (VARCHAR UNIQUE)`, `password_hash (VARCHAR)`, `role (VARCHAR: USER | ADMIN)`, `created_at (TIMESTAMPTZ)`.

2. **`events`**
   - The flash sale entity.
   - Fields: `id (BIGSERIAL PK)`, `name (VARCHAR)`, `description (TEXT)`, `total_tickets (INT)`, `tickets_sold (INT)`, `sale_start_time (TIMESTAMPTZ)`, `waiting_room_open_offset_seconds (INT)`, `status (VARCHAR: UPCOMING | LIVE | ENDED | CANCELLED)`, `queue_finalized_at (TIMESTAMPTZ)`, `created_at (TIMESTAMPTZ)`.
   - Invariant: `tickets_sold <= total_tickets`.

3. **`queue_entries`**
   - Represents a user's participation in an event's waiting room or queue.
   - Fields: `id (BIGSERIAL PK)`, `event_id (FK events)`, `user_id (FK users)`, `joined_at (TIMESTAMPTZ)`, `queue_position (BIGINT, nullable)`, `status (VARCHAR: WAITING | QUEUED | ADMITTED | EXPIRED | COMPLETED)`, `admitted_at (TIMESTAMPTZ)`, `admission_expires_at (TIMESTAMPTZ)`.
   - Invariant: `UNIQUE(event_id, user_id)` ensures a user holds exactly one position per event.

4. **`bookings`**
   - The confirmed ticket purchase record.
   - Fields: `id (BIGSERIAL PK)`, `user_id (FK users)`, `event_id (FK events)`, `status (VARCHAR: CONFIRMED | CANCELLED)`, `booked_at (TIMESTAMPTZ)`.
   - Invariant: `UNIQUE(user_id, event_id)` guarantees one ticket booking per user per event.

---

## 2. Lifecycle State Machines

### Event Lifecycle
```
[ UPCOMING ] ──(sale_start_time reached)──> [ LIVE ] ──(sold out / time elapsed)──> [ ENDED ]
      │                                       │
(admin cancel)                          (admin cancel)
      │                                       │
      └─────────────────> [ CANCELLED ] <──────┘
```
- **UPCOMING -> LIVE**: Handled by periodic background scheduler and dynamic on-read fallback (`now >= sale_start_time`).
- **LIVE -> ENDED**: When `tickets_sold >= total_tickets`.
- **CANCELLED**: Terminal administrative state; cannot be reversed.

### Queue Entry Lifecycle
```
[ WAITING ] ──(sale start randomizer)──> [ QUEUED ]
                                              │
                                   (batch admission)
                                              │
                                              v
[ EXPIRED ] <──(window passed)─────────── [ ADMITTED ]
                                              │
                                       (successful book)
                                              │
                                              v
                                         [ COMPLETED ]
```
- **WAITING**: User joined during waiting room window prior to sale start.
- **QUEUED**: Assigned an ordered sequence number (`queue_position`).
- **ADMITTED**: User has an active, time-limited window to execute the checkout flow.
- **EXPIRED**: The user failed to book within their admission window.
- **COMPLETED**: The ticket was successfully reserved and purchased.

---

## 3. Concurrency Guarantees & Safety Rationale

### 3.1 Absolute Oversell Prevention
Rather than relying on application-level locks, distributed locks (Redis), or table-level locks that cause massive contention, HotDrop relies on PostgreSQL row-level MVCC semantics:

```sql
UPDATE events
SET tickets_sold = tickets_sold + 1
WHERE id = :eventId AND tickets_sold < total_tickets
RETURNING tickets_sold;
```
- If the query returns 1 row: Ticket is safely reserved; proceed to insert `bookings` record.
- If the query returns 0 rows: The event is completely sold out. Roll back / reject request with HTTP 409.
- This guarantee is atomic, deadlock-free, and safe under arbitrary thread counts across multiple application nodes.

### 3.2 Fair Queue Shuffle
To prevent unfair latency advantages during connection establishment:
- Users connect to a waiting room up to `waiting_room_open_offset_seconds` prior to sale start.
- At `sale_start_time`, an atomic update shuffles all waiting users using PostgreSQL's `random()` function within an atomic CTE:
  ```sql
  WITH shuffled AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY random()) as pos
    FROM queue_entries
    WHERE event_id = :eventId AND status = 'WAITING'
  )
  UPDATE queue_entries q
  SET queue_position = s.pos, status = 'QUEUED'
  FROM shuffled s WHERE q.id = s.id;
  ```
- Any user joining after `queue_finalized_at` is appended sequentially (`MAX(queue_position) + 1`).

### 3.3 Throttled Admission (`FOR UPDATE SKIP LOCKED`)
To protect downstream booking handlers from being flooded by thousands of concurrent requests:
- Admissions occur in controlled batches of size `N`.
- The admission query selects candidate rows using `FOR UPDATE SKIP LOCKED`, allowing safe multi-node worker concurrency without lock contention:
  ```sql
  SELECT id FROM queue_entries
  WHERE event_id = :eventId AND status = 'QUEUED'
  ORDER BY queue_position ASC
  LIMIT :batchSize
  FOR UPDATE SKIP LOCKED;
  ```
- Selected entries are updated to `ADMITTED` with an expiration timestamp (`NOW() + INTERVAL '120 seconds'`).
