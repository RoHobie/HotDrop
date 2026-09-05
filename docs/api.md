# HotDrop REST API Specification

Base URL: `http://localhost:8081` (default local host mapping)

## Standard Response & Headers
- `Content-Type: application/json`
- Authenticated endpoints require: `Authorization: Bearer <jwt-token>`
- Errors adhere to structured error formats with fields:
  ```json
  {
    "timestamp": "2026-09-05T08:30:00Z",
    "status": 400,
    "error": "Bad Request",
    "message": "Validation error or business logic rejection",
    "path": "/events/1/book"
  }
  ```

---

## 1. System & Health

### Health Check
`GET /health`  
Returns server liveness.
- **Response (200 OK):**
  ```json
  {
    "status": "UP"
  }
  ```

### Actuator Health
`GET /actuator/health`  
Returns detailed subsystem status (database, disk, etc.).
- **Response (200 OK):**
  ```json
  {
    "status": "UP",
    "components": {
      "db": { "status": "UP" },
      "diskSpace": { "status": "UP" }
    }
  }
  ```

---

## 2. Authentication

### User Registration
`POST /auth/signup`
- **Request Body:**
  ```json
  {
    "name": "Alice Johnson",
    "email": "alice@example.com",
    "password": "Password123!"
  }
  ```
- **Response (201 Created):**
  ```json
  {
    "id": 1,
    "name": "Alice Johnson",
    "email": "alice@example.com",
    "role": "USER",
    "createdAt": "2026-09-05T08:30:00Z"
  }
  ```

### User Login
`POST /auth/login`
- **Request Body:**
  ```json
  {
    "email": "alice@example.com",
    "password": "Password123!"
  }
  ```
- **Response (200 OK):**
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "user": {
      "id": 1,
      "email": "alice@example.com",
      "name": "Alice Johnson",
      "role": "USER"
    }
  }
  ```

### User Logout
`POST /auth/logout`
- **Headers:** `Authorization: Bearer <jwt-token>`
- **Response (200 OK):**
  ```json
  {
    "message": "Logged out successfully"
  }
  ```

---

## 3. Events

### Create Event (Admin Only)
`POST /admin/events`
- **Headers:** `Authorization: Bearer <admin-jwt-token>`
- **Request Body:**
  ```json
  {
    "name": "Coldplay World Tour 2026",
    "description": "Exclusive presale tickets",
    "totalTickets": 500,
    "saleStartTime": "2026-09-05T15:00:00Z",
    "waitingRoomOpenOffsetSeconds": 900
  }
  ```
- **Response (201 Created):**
  ```json
  {
    "id": 1,
    "name": "Coldplay World Tour 2026",
    "description": "Exclusive presale tickets",
    "totalTickets": 500,
    "ticketsSold": 0,
    "saleStartTime": "2026-09-05T15:00:00Z",
    "waitingRoomOpenOffsetSeconds": 900,
    "status": "UPCOMING",
    "createdAt": "2026-09-05T08:30:00Z"
  }
  ```

### Cancel Event (Admin Only)
`PATCH /admin/events/{id}/cancel`
- **Headers:** `Authorization: Bearer <admin-jwt-token>`
- **Response (200 OK):**
  ```json
  {
    "id": 1,
    "status": "CANCELLED",
    "message": "Event cancelled successfully"
  }
  ```

### List Events (Public)
`GET /events?status=upcoming`
- **Query Params:** `status` (optional: `UPCOMING`, `LIVE`, `ENDED`, `CANCELLED`)
- **Response (200 OK):** Array of event objects.

### Get Event Details (Public)
`GET /events/{id}`
- **Response (200 OK):** Event details with dynamically evaluated status.

---

## 4. Waiting Room & Queue

### Join Waiting Room / Queue
`POST /events/{id}/waiting-room/join`
- **Headers:** `Authorization: Bearer <jwt-token>`
- **Response (200 OK):**
  ```json
  {
    "eventId": 1,
    "userId": 1,
    "status": "WAITING",
    "joinedAt": "2026-09-05T14:50:00Z"
  }
  ```

### Check Waiting Room Status
`GET /events/{id}/waiting-room/status`
- **Headers:** `Authorization: Bearer <jwt-token>`
- **Response (200 OK):**
  ```json
  {
    "eventId": 1,
    "userId": 1,
    "status": "WAITING",
    "secondsUntilSale": 420
  }
  ```

### Check Queue Status & Admission
`GET /events/{id}/queue/status`
- **Headers:** `Authorization: Bearer <jwt-token>`
- **Response (200 OK):**
  ```json
  {
    "eventId": 1,
    "userId": 1,
    "status": "ADMITTED",
    "queuePosition": 4,
    "totalInQueue": 350,
    "admittedAt": "2026-09-05T15:00:05Z",
    "admissionExpiresAt": "2026-09-05T15:02:05Z",
    "secondsRemaining": 115
  }
  ```

---

## 5. Bookings

### Book Ticket
`POST /events/{id}/book`
- **Headers:** `Authorization: Bearer <jwt-token>`
- **Response (200 OK):**
  ```json
  {
    "bookingId": 42,
    "eventId": 1,
    "eventName": "Coldplay World Tour 2026",
    "userId": 1,
    "status": "CONFIRMED",
    "bookedAt": "2026-09-05T15:00:30Z"
  }
  ```
- **Error (409 Conflict):** When sold out:
  ```json
  {
    "error": "Conflict",
    "message": "Event is sold out"
  }
  ```

### List My Bookings
`GET /users/me/bookings`
- **Headers:** `Authorization: Bearer <jwt-token>`
- **Response (200 OK):** Array of user's confirmed bookings.

---

## 6. Admin Analytics

### Per-Event Sales Summary
`GET /admin/events/{id}/sales`
- **Headers:** `Authorization: Bearer <admin-jwt-token>`
- **Response (200 OK):**
  ```json
  {
    "eventId": 1,
    "eventName": "Coldplay World Tour 2026",
    "totalTickets": 500,
    "ticketsSold": 500,
    "remainingTickets": 0,
    "status": "ENDED",
    "totalInQueue": 1200,
    "completedBookings": 500
  }
  ```

### Platform Sales Summary
`GET /admin/sales/summary`
- **Headers:** `Authorization: Bearer <admin-jwt-token>`
- **Response (200 OK):**
  ```json
  {
    "totalEvents": 12,
    "totalTicketsAvailable": 6000,
    "totalTicketsSold": 5850,
    "activeEventsCount": 3
  }
  ```
