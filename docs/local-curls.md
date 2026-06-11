# Local cURLs — ulyses.technical.test

Collection of `curl` commands to exercise the API locally after starting the
service (`./mvnw spring-boot:run`). Ordered so that write operations always
target resources created within the same session and do not conflict with the
foreign key constraints defined in `data.sql`.

> **Base URL**: `http://localhost:8080`
> **Auth**: `admin:admin` (write operations), `user:user` (read-only).
> **Preloaded data**: brands 1–3 (Renault, Opel, VW), vehicles 1–9, ~50 sales
> per vehicle in January 2025.
> **Convention**: IDs 1–3 (brands) and 1–9 (vehicles) are **read-only**.
> All write operations must target resources created in the `*.CREATE` steps
> (IDs ≥ 4 for brands, ≥ 10 for vehicles).

---

## 0. Health / H2 console

```bash
# H2 console (browser): http://localhost:8080/h2-console
#   JDBC URL: jdbc:h2:mem:testdb  ·  user: sa  ·  password: password

curl -i http://localhost:8080/api/brands   # connectivity check
```

---

## 1. Public read operations (order-independent, no state mutation)

### 1.1 Brands
```bash
curl -i http://localhost:8080/api/brands                # populates "all" cache entry
curl -i http://localhost:8080/api/brands                # 2nd request → served from cache
curl -i http://localhost:8080/api/brands/1              # cache by id
curl -i http://localhost:8080/api/brands/2
curl -i http://localhost:8080/api/brands/3
curl -i http://localhost:8080/api/brands/9999           # 404
```

### 1.2 Vehicles
```bash
curl -i http://localhost:8080/api/vehicles
curl -i http://localhost:8080/api/vehicles/1            # Clio
curl -i http://localhost:8080/api/vehicles/9            # Tiguan
curl -i http://localhost:8080/api/vehicles/9999         # 404
```

### 1.3 Sales (pagination + filters + best-selling)
```bash
# Pagination
curl -i http://localhost:8080/api/sales                 # default page=1
curl -i 'http://localhost:8080/api/sales?page=1'
curl -i 'http://localhost:8080/api/sales?page=2'
curl -i 'http://localhost:8080/api/sales?page=50'
curl -i 'http://localhost:8080/api/sales?page=99999'    # 200, data=[], hasMore=false

# Validation errors
curl -i 'http://localhost:8080/api/sales?page=0'        # 400
curl -i 'http://localhost:8080/api/sales?page=-1'       # 400
curl -i 'http://localhost:8080/api/sales?page=abc'      # 400

# Detail
curl -i http://localhost:8080/api/sales/1
curl -i http://localhost:8080/api/sales/9999999         # 404
curl -i http://localhost:8080/api/sales/0               # 400

# Filter by brand / vehicle
curl -i http://localhost:8080/api/sales/brands/1
curl -i http://localhost:8080/api/sales/brands/2
curl -i http://localhost:8080/api/sales/brands/3
curl -i http://localhost:8080/api/sales/brands/-3       # 400
curl -i http://localhost:8080/api/sales/vehicles/1
curl -i http://localhost:8080/api/sales/vehicles/9
curl -i http://localhost:8080/api/sales/vehicles/0      # 400

# Best-selling vehicles
curl -i http://localhost:8080/api/sales/vehicles/bestSelling
curl -i 'http://localhost:8080/api/sales/vehicles/bestSelling?startDate=2025-01-01&endDate=2025-01-02'
curl -i 'http://localhost:8080/api/sales/vehicles/bestSelling?startDate=2025-01-01&endDate=2025-01-31'
curl -i 'http://localhost:8080/api/sales/vehicles/bestSelling?startDate=2025-01-10'
curl -i 'http://localhost:8080/api/sales/vehicles/bestSelling?endDate=2025-01-05'
curl -i 'http://localhost:8080/api/sales/vehicles/bestSelling?startDate=2025-02-01&endDate=2025-01-01'  # 400 (inverted range)
curl -i 'http://localhost:8080/api/sales/vehicles/bestSelling?startDate=01-01-2025'                     # 400 (wrong format)
```

---

## 2. Security checks (no relevant state mutation)

```bash
# Write without credentials → 401
curl -i -X POST   http://localhost:8080/api/brands  -H 'Content-Type: application/json' -d '{}'
curl -i -X DELETE http://localhost:8080/api/brands/1

# Write with USER role → 403
curl -i -u user:user -X POST   http://localhost:8080/api/brands  -H 'Content-Type: application/json' -d '{"name":"x","description":"x"}'
curl -i -u user:user -X DELETE http://localhost:8080/api/brands/1

# Wrong credentials → 401
curl -i -u admin:wrong -X PUT http://localhost:8080/api/brands/1 -H 'Content-Type: application/json' -d '{"name":"x","description":"x"}'
```
> The `DELETE /api/brands/1` and `PUT /api/brands/1` commands above **fail
> before reaching the database** (401/403), so they are safe even though they
> target preloaded IDs.

---

## 3. Write operations (order matters — always use self-created IDs)

> ⚠️ Follow the order. Each block creates a resource and reuses its ID in
> subsequent steps. Assumes the first `POST /api/brands` returns ID `4` and
> the first `POST /api/vehicles` returns ID `10`. If you have made prior
> creations in the same session, adjust the IDs to the values returned in the
> `Location` header or response body.

### 3.1 Brands — create, update, delete (own brand only)
```bash
# 3.1.1 CREATE → 201, evicts "all" cache entry
curl -i -u admin:admin -X POST http://localhost:8080/api/brands \
  -H 'Content-Type: application/json' \
  -d '{"name":"Peugeot","description":"French manufacturer"}'

# 3.1.2 GET to confirm the returned ID (assumed 4)
curl -i http://localhost:8080/api/brands/4

# 3.1.3 UPDATE a brand without vehicles → 200 (safe, does not trigger orphanRemoval FK violations)
curl -i -u admin:admin -X PUT http://localhost:8080/api/brands/4 \
  -H 'Content-Type: application/json' \
  -d '{"name":"Peugeot-Updated","description":"edited"}'

# 3.1.4 UPDATE non-existent ID → 404
curl -i -u admin:admin -X PUT http://localhost:8080/api/brands/9999 \
  -H 'Content-Type: application/json' \
  -d '{"name":"x","description":"x"}'

# 3.1.5 DELETE the brand just created → 204
curl -i -u admin:admin -X DELETE http://localhost:8080/api/brands/4

# 3.1.6 DELETE non-existent ID → 404
curl -i -u admin:admin -X DELETE http://localhost:8080/api/brands/9999
```

### 3.2 Vehicles — create, update, delete (own vehicle only)
```bash
# 3.2.1 CREATE under an existing brand (id=1, reference only) → 201
curl -i -u admin:admin -X POST http://localhost:8080/api/vehicles \
  -H 'Content-Type: application/json' \
  -d '{"brand":{"id":1},"model":"Kadjar","year":"2024","color":"Gray"}'

# 3.2.2 GET to confirm the returned ID (assumed 10)
curl -i http://localhost:8080/api/vehicles/10

# 3.2.3 CREATE with non-existent brand → 400
curl -i -u admin:admin -X POST http://localhost:8080/api/vehicles \
  -H 'Content-Type: application/json' \
  -d '{"brand":{"id":9999},"model":"Ghost","year":"2024","color":"None"}'

# 3.2.4 UPDATE own vehicle → 200
curl -i -u admin:admin -X PUT http://localhost:8080/api/vehicles/10 \
  -H 'Content-Type: application/json' \
  -d '{"brand":{"id":1},"model":"Kadjar GT","year":"2024","color":"Black"}'

# 3.2.5 UPDATE non-existent ID → 404
curl -i -u admin:admin -X PUT http://localhost:8080/api/vehicles/9999 \
  -H 'Content-Type: application/json' \
  -d '{"brand":{"id":1},"model":"X","year":"2024","color":"Black"}'

# 3.2.6 DELETE own vehicle → 204 (no associated sales)
curl -i -u admin:admin -X DELETE http://localhost:8080/api/vehicles/10
```

---

## ⚠️ Known limitations (out of scope — documented, not fixed)

The following are behaviours inherited from the original scaffold. They can
break a request if the execution order above is not respected. They are **not
covered by the assignment README** and therefore fall outside the scope of this
implementation.

### G1. `PUT /api/brands/{id}` on a brand that has vehicles → 500 (FK violation)
Example:
```bash
# ❌ DO NOT target IDs 1, 2 or 3 (they have associated vehicles and, transitively, sales)
curl -i -u admin:admin -X PUT http://localhost:8080/api/brands/1 \
  -H 'Content-Type: application/json' \
  -d '{"name":"Renault-Updated","description":"edited"}'
```
- **Symptom**: `org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException` on
  `FK ... SALES.VEHICLE_ID → VEHICLES.ID`.
- **Root cause**: `Brand.vehicles` is declared with `cascade = ALL, orphanRemoval = true`.
  The controller calls `brand.setId(id); save(brand);` with a `Brand` object
  deserialized from JSON that **does not include `vehicles`** → JPA interprets
  the collection as empty → attempts to delete existing vehicles → any vehicle
  referenced by a `sales` row violates the FK constraint.
- **Workarounds** (without touching the code):
  - Only run PUT against brands you created yourself **with no associated
    vehicles** (steps 3.1.x).
  - Or explicitly include the vehicle references in the request body:
    ```bash
    curl -i -u admin:admin -X PUT http://localhost:8080/api/brands/1 \
      -H 'Content-Type: application/json' \
      -d '{"name":"Renault-Updated","description":"edited","vehicles":[{"id":1},{"id":2},{"id":3}]}'
    ```

### G2. `DELETE /api/vehicles/{id}` on a vehicle with associated sales → 500 (FK violation)
- **Symptom**: same type of integrity violation on `SALES.VEHICLE_ID`.
- **Root cause**: there is no cascade from `Vehicle` to `Sales`. Deleting a
  vehicle referenced by rows in `sales` fails.
- **Workaround**: only delete vehicles created by you (step 3.2.6), never IDs 1–9.

### G3. `DELETE /api/brands/{id}` on a brand with vehicles
- **Symptom**: `Brand.vehicles` cascades ALL, so deletion propagates to
  vehicles → which then collide with the `sales` FK if the brand has
  associated sales.
- **Workaround**: only delete brands created by you, with no vehicles or
  sales attached (step 3.1.5).

### G4. Brand cache and manual H2 console mutations
If data is modified directly from `/h2-console`, the brand cache (TTL 60 s)
is not notified. Wait for the TTL to expire or trigger a `POST/PUT/DELETE`
on `/api/brands/**` to force cache eviction.

### G5. Invalid GETs returning 401 instead of 400/404 — **RESOLVED**
- **Original symptom**: `GET /api/sales?page=0`, `GET /api/sales/0`,
  `GET /api/sales/brands/-3`, `GET /api/sales/vehicles/bestSelling` with an
  inverted date range, etc. → clients received `401` with
  `WWW-Authenticate: Basic`, while `logs/access.log` correctly recorded `400`.
- **Root cause**: when a public endpoint throws a `ResponseStatusException`,
  Spring performs an internal `FORWARD` to `/error`. The Security filter chain
  re-evaluates that path, and `/error` was not included in `permitAll()` →
  it fell through to `anyRequest().authenticated()` → 401.
- **Fix applied** in `SecurityConfig.securityFilterChain`:
  ```java
  .requestMatchers("/error").permitAll()
  ```
  This is strictly outside the assignment README scope, but it is a trivial,
  low-risk change: `/error` is a Spring internal endpoint reached only via
  container dispatch, does not expose sensitive information, and does not
  weaken the `POST/PUT/DELETE` rules (they still require `ADMIN`). Without
  it, any query/path validation on a GET endpoint is unusable by external
  clients.
- **Not caught by ITs** because `MockMvc` does not process the `/error`
  dispatch the same way a real servlet container does.

---

## 4. Access log verification

```bash
tail -n 20 logs/access.log
```
Each request should appear with its timestamp, HTTP method, URL, response
status, and duration in milliseconds.
