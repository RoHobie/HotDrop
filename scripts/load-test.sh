#!/usr/bin/env bash
set -eo pipefail

BASE_URL="${1:-http://localhost:8081}"
USERS_COUNT="${2:-100}"
TICKETS_COUNT="${3:-20}"

echo "=========================================================="
echo " HotDrop Mass-Concurrency Flash Sale Load Test"
echo " Target: $BASE_URL"
echo " Prospective Buyers: $USERS_COUNT"
echo " Available Tickets:  $TICKETS_COUNT"
echo "=========================================================="

# 1. Health check
echo "[1/6] Checking system health..."
curl -s -f "$BASE_URL/health" > /dev/null || { echo "Error: App not reachable at $BASE_URL"; exit 1; }
echo " -> System is UP"

# 2. Register & Login Admin
echo "[2/6] Registering Admin..."
ADMIN_EMAIL="admin_load_${RANDOM}@hotdrop.local"
curl -s -X POST "$BASE_URL/auth/signup" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"LoadAdmin\",\"email\":\"$ADMIN_EMAIL\",\"password\":\"AdminPass123!\",\"role\":\"ADMIN\"}" > /dev/null

ADMIN_TOKEN=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"AdminPass123!\"}" | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$ADMIN_TOKEN" ]; then
  echo "Error: Failed to obtain admin token"
  exit 1
fi
echo " -> Admin authenticated"

# 3. Create Event starting in 3 seconds
echo "[3/6] Creating Flash Sale Event..."
SALE_START=$(date -u -d "+3 seconds" +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -v+3S +%Y-%m-%dT%H:%M:%SZ)
EVENT_PAYLOAD="{\"name\":\"Mega Stadium Tour\",\"description\":\"Heavy Concurrency Test\",\"totalTickets\":$TICKETS_COUNT,\"saleStartTime\":\"$SALE_START\",\"waitingRoomOpenOffsetSeconds\":300}"

EVENT_RESP=$(curl -s -X POST "$BASE_URL/admin/events" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "$EVENT_PAYLOAD")

EVENT_ID=$(echo "$EVENT_RESP" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
echo " -> Event created with ID: $EVENT_ID (Tickets: $TICKETS_COUNT, Starts at: $SALE_START)"

# 4. Register Users and Join Waiting Room in Parallel
echo "[4/6] Registering $USERS_COUNT users and joining waiting room..."
TOKENS_FILE=$(mktemp)
trap 'rm -f "$TOKENS_FILE"' EXIT

for i in $(seq 1 "$USERS_COUNT"); do
  (
    EMAIL="buyer_${i}_${RANDOM}@hotdrop.local"
    curl -s -X POST "$BASE_URL/auth/signup" \
      -H "Content-Type: application/json" \
      -d "{\"name\":\"Buyer $i\",\"email\":\"$EMAIL\",\"password\":\"Pass123!\",\"role\":\"USER\"}" > /dev/null

    TOKEN=$(curl -s -X POST "$BASE_URL/auth/login" \
      -H "Content-Type: application/json" \
      -d "{\"email\":\"$EMAIL\",\"password\":\"Pass123!\"}" | grep -o '"token":"[^"]*' | cut -d'"' -f4)

    if [ -n "$TOKEN" ]; then
      echo "$TOKEN" >> "$TOKENS_FILE"
      curl -s -X POST "$BASE_URL/events/$EVENT_ID/waiting-room/join" \
        -H "Authorization: Bearer $TOKEN" > /dev/null
    fi
  ) &
  if (( i % 25 == 0 )); then wait; fi
done
wait
echo " -> All users joined waiting room"

# 5. Wait for sale time and randomization
echo "[5/6] Waiting for sale to begin and queue finalization..."
sleep 4

# 6. Hammer Booking Endpoint concurrently
echo "[6/6] Hammering booking endpoint with all users concurrently..."
START_TIME=$(date +%s%N)
SUCCESS_COUNT=0
SOLDOUT_COUNT=0

BOOK_RESULTS=$(mktemp)
trap 'rm -f "$TOKENS_FILE" "$BOOK_RESULTS"' EXIT

while IFS= read -r USER_TOKEN; do
  (
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/events/$EVENT_ID/book" \
      -H "Authorization: Bearer $USER_TOKEN")
    echo "$HTTP_CODE" >> "$BOOK_RESULTS"
  ) &
done < "$TOKENS_FILE"
wait

END_TIME=$(date +%s%N)
DURATION_MS=$(( (END_TIME - START_TIME) / 1000000 ))

SUCCESS_COUNT=$(grep -c "200" "$BOOK_RESULTS" || true)
CONFLICT_COUNT=$(grep -c "409" "$BOOK_RESULTS" || true)
BAD_REQ_COUNT=$(grep -c "400" "$BOOK_RESULTS" || true)

echo "----------------------------------------------------------"
echo " Concurrency Results:"
echo " Total Requests:   $USERS_COUNT"
echo " HTTP 200 Success: $SUCCESS_COUNT"
echo " HTTP 409 SoldOut: $CONFLICT_COUNT"
echo " Non-admitted 400: $BAD_REQ_COUNT"
echo " Total Time:       ${DURATION_MS}ms"

# 7. Verify via Admin Sales Endpoint
echo "----------------------------------------------------------"
echo " Database Verification:"
SALES_RESP=$(curl -s -X GET "$BASE_URL/admin/events/$EVENT_ID/sales" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

echo "$SALES_RESP"

TICKETS_SOLD=$(echo "$SALES_RESP" | grep -o '"ticketsSold":[0-9]*' | cut -d':' -f2)
COMPLETED_BOOKINGS=$(echo "$SALES_RESP" | grep -o '"completedBookings":[0-9]*' | cut -d':' -f2)

if [ "$TICKETS_SOLD" -le "$TICKETS_COUNT" ] && [ "$TICKETS_SOLD" -eq "$COMPLETED_BOOKINGS" ]; then
  echo ">>> VERIFICATION PASSED: No overselling occurred! Tickets sold ($TICKETS_SOLD) <= Capacity ($TICKETS_COUNT) <<<"
else
  echo ">>> VERIFICATION FAILED: Tickets sold ($TICKETS_SOLD) vs Capacity ($TICKETS_COUNT) <<<"
  exit 1
fi
