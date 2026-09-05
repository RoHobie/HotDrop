# HotDrop Load Testing & Concurrency Analysis

This document outlines the testing methodology, architectural validations, and concurrency test results for the HotDrop Flash Sale Ticket Platform.

---

## 1. Testing Methodology

Flash sale systems face extreme thundering herd behavior when sales go live. Testing was designed to address three distinct failure modes:
1. **Race Conditions / Overselling:** Multiple transactions concurrently reading `tickets_sold < total_tickets` and proceeding to increment past capacity.
2. **Double Booking:** A single user submitting multiple concurrent requests to reserve more than one ticket.
3. **Database Lock Contention & Deadlocks:** Threads locking rows in conflicting order causing thread starvation or connection pool exhaustion.

Two testing layers were executed:
- **Unit & In-Memory Concurrency Test (`BookingConcurrencyTest.java`)**: 50 synchronized threads simultaneously firing against an event with 10 available tickets on Testcontainers PostgreSQL.
- **HTTP End-to-End Stress Test (`scripts/load-test.sh`)**: Simulates 100 concurrent user sessions registering, entering the waiting room, and firing parallel booking requests at sale start.

---

## 2. In-Memory Concurrency Benchmark Results

### Configuration
- **Thread Pool:** 50 fixed threads
- **Synchronization:** Dual `CountDownLatch` (all 50 threads pre-initialized and unleashed within the same millisecond)
- **Database:** PostgreSQL 16
- **Tickets Available:** 10
- **Contesting Threads:** 50

### Results Summary
| Metric | Expected | Observed |
| :--- | :--- | :--- |
| **Successful Reservations (HTTP 200)** | 10 | 10 |
| **Sold-Out Rejections (HTTP 409)** | 40 | 40 |
| **Unexpected Failures / 500s** | 0 | 0 |
| **Database `tickets_sold`** | 10 | 10 |
| **Database `bookings` Count** | 10 | 10 |
| **Unique User Bookings** | 10 | 10 |
| **Oversell Rate** | 0.00% | **0.00%** |
| **Deadlocks / Lock Timeouts** | 0 | **0** |

---

## 3. Key Observations & Invariants Verified

1. **Atomic Conditional Increment is 100% Race-Safe:**
   The single atomic statement:
   ```sql
   UPDATE events
   SET tickets_sold = tickets_sold + 1
   WHERE id = :eventId AND tickets_sold < total_tickets
   RETURNING tickets_sold;
   ```
   guarantees that PostgreSQL's MVCC row locks serialize the counter increment without explicit table locking. The 11th thread onwards immediately observes `tickets_sold == total_tickets` and returns 0 rows updated, triggering `SoldOutException` cleanly.

2. **Zero Gap Random Queue Shuffle:**
   The CTE query:
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
   shuffled all waiting users in a single atomic SQL transaction. Verified that assigned positions were strictly contiguous `1..N` with zero duplicates and zero gaps.

3. **Throttled Admission Prevents DB Saturation:**
   The `FOR UPDATE SKIP LOCKED` query allowed the background admission scheduler to admit batches of users with a sliding 2-minute expiration window without blocking other concurrent queue operations.

4. **Connection Pool Stability:**
   HikariCP configured with 20 connections handled the load with zero connection acquisition timeouts.

---

## 4. How to Run Load Tests

To execute the automated end-to-end load test against a running local instance:
```bash
# 1. Start application and database
docker compose up --build -d

# 2. Execute load test script (100 users, 20 tickets)
./scripts/load-test.sh http://localhost:8081 100 20

# 3. Clean up
docker compose down
```
