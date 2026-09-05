# HotDrop — Flash Sale Ticket Platform

HotDrop is a high-concurrency flash sale ticket platform designed to handle massive traffic spikes with absolute fairness and zero overselling.

## Tech Stack
- **Language & Runtime:** Java 21
- **Framework:** Spring Boot 4.1.1 (WebMVC, Data JPA, Security, Validation, Actuator)
- **Database:** PostgreSQL 16
- **Database Migrations:** Flyway
- **Authentication:** Stateless JWT with BCrypt password hashing
- **Testing:** JUnit 5, Spring Boot Test, Testcontainers (PostgreSQL)
- **Containerization:** Docker & Docker Compose (Multi-stage build)

---

## Architectural Principles

1. **Absolute Anti-Overselling Guarantee**:
   Preventing overselling is enforced at the database level using a single atomic conditional update:
   ```sql
   UPDATE events
   SET tickets_sold = tickets_sold + 1
   WHERE id = :eventId AND tickets_sold < total_tickets
   RETURNING tickets_sold;
   ```
   No tickets can ever be oversold regardless of application concurrency or thread counts.

2. **Fair Waiting Room & Queue Shuffle**:
   Users who join before the sale starts enter a waiting room. At the exact sale start time, all waiting users are atomically assigned random queue positions via:
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
   Users arriving after sale start are appended to the back of the queue (`MAX(queue_position) + 1`).

3. **Admission Throttling (`FOR UPDATE SKIP LOCKED`)**:
   Queued users are admitted in batches with a time-limited purchase window (e.g. 2 minutes), protecting checkout endpoints from thundering herds.

---

## Getting Started

### Prerequisites
- Docker and Docker Compose
- (Optional for local development without Docker) Java 21 and Maven 3.9+

### Environment Configuration
Copy the sample environment file:
```bash
cp .env.example .env
```
Default ports:
- Application: `8081` (host) -> `8080` (container)
- PostgreSQL: `5436` (host) -> `5432` (container)

### Running with Docker Compose
```bash
docker compose up --build -d
```
Verify the health check:
```bash
curl http://localhost:8081/health
curl http://localhost:8081/actuator/health
```

### Running Tests
All tests run against real PostgreSQL instances via Testcontainers:
```bash
./mvnw clean test
```

---

## Project Structure
```
├── docs/
│   ├── api.md            # API endpoints specification and curl examples
│   ├── architecture.md   # Architectural design, state machines, concurrency guarantees
│   └── load-testing.md   # Concurrency and load testing reports
├── src/
│   ├── main/
│   │   ├── java/com/hotdrop/
│   │   │   ├── config/       # Security and application configurations
│   │   │   ├── user/         # User domain & authentication
│   │   │   ├── event/        # Event lifecycle management
│   │   │   ├── queue/        # Waiting room and queue logic
│   │   │   ├── booking/      # Ticket reservation & overselling prevention
│   │   │   ├── admin/        # Sales reporting and admin metrics
│   │   │   └── health/       # Health check controller
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/ # Flyway migrations
│   └── test/
│       └── java/com/hotdrop/ # Integration and concurrency test suites
├── Dockerfile
├── docker-compose.yml
└── pom.xml
```
