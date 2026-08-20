# Book Lending Microservice

A self-contained Java microservice for managing a book lending system.

Repository: https://github.com/chndranndr/book-lending

## Running

```bash
docker compose up --build
```

The application is available at http://localhost:8080

Docker is the recommended execution path, so the reviewer does not need a matching local Java or Maven installation.

## Stopping

```bash
docker compose down
```

## Resetting data

```bash
docker compose down -v
```

The SQLite database is stored in a Docker volume, so normal container recreation does not remove application data.

## Authentication

HTTP Basic authentication with predefined users:

| Username    | Password            | Role      |
|-------------|---------------------|-----------|
| admin       | admin-password      | ADMIN     |
| librarian   | librarian-password  | LIBRARIAN |

**ADMIN** can manage books, members, and loans.  
**LIBRARIAN** can read books/members and borrow/return books.

Swagger UI exposes an **Authorize** action for the same HTTP Basic credentials.

## API Documentation

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## Configuration

| Key                                      | Default | Description                     |
|------------------------------------------|---------|---------------------------------|
| `library.borrowing.max-active-loans`     | 3       | Maximum active loans per member |
| `library.borrowing.loan-duration-days`   | 14      | Loan duration in days           |

The values can be overridden when using Docker Compose:

```bash
LIBRARY_BORROWING_MAX_ACTIVE_LOANS=5 \
LIBRARY_BORROWING_LOAN_DURATION_DAYS=21 \
docker compose up --build
```

On PowerShell:

```powershell
$env:LIBRARY_BORROWING_MAX_ACTIVE_LOANS="5"
$env:LIBRARY_BORROWING_LOAN_DURATION_DAYS="21"
docker compose up --build
```

## Useful Endpoints

| Endpoint            | Description         | Authentication |
|---------------------|---------------------|----------------|
| `/swagger-ui.html`  | Swagger UI          | Public         |
| `/v3/api-docs`      | OpenAPI JSON        | Public         |
| `/actuator/health`  | Health check        | Public         |
| `/actuator/metrics` | Application metrics | Required       |

## Running with Maven (optional)

Requires Java 21 locally.

```bash
./mvnw spring-boot:run
```

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

## Quick API flow

Replace the example IDs with IDs returned by the previous response:

```bash
# Create a book
curl -u admin:admin-password -H "Content-Type: application/json" \
  -d '{"title":"Clean Code","author":"Robert C. Martin","isbn":"9780132350884","totalCopies":1}' \
  http://localhost:8080/api/books

# Create a member
curl -u admin:admin-password -H "Content-Type: application/json" \
  -d '{"name":"Alice","email":"alice@example.com"}' \
  http://localhost:8080/api/members

# Borrow, then return
curl -u admin:admin-password -H "Content-Type: application/json" \
  -d '{"bookId":1,"memberId":1}' http://localhost:8080/api/loans
curl -u admin:admin-password -X POST http://localhost:8080/api/loans/1/return
```

## Design Decisions

### SQLite with Docker

SQLite keeps the service self-contained while still satisfying the relational database requirement. It avoids requiring the evaluator to provision a separate database server.

The application is still packaged with Docker so the runtime Java version is fixed. The SQLite file is stored in a named Docker volume so data survives container recreation.

### Inventory updates

Borrowing does not read `availableCopies`, check it in Java, and then write it back. The repository performs a conditional database update:

```sql
UPDATE book
SET available_copies = available_copies - 1
WHERE id = :bookId
  AND available_copies > 0;
```

A zero-row update is treated as `BOOK_UNAVAILABLE`. This avoids a check-then-update race for the last available copy without introducing Redis or a distributed lock.

### Time-dependent rules

The service injects `java.time.Clock` instead of spreading direct `Instant.now()` calls through the lending logic. Tests can replace it with a fixed clock, making due-date calculation and overdue checks deterministic.

### Borrowing configuration

Maximum active loans and loan duration are application configuration because the assignment explicitly requires borrowing rules not to be stored in the database.

### Duplicate returns

A loan that already has `returnedAt` is returned unchanged. This prevents a repeated return request from incrementing `availableCopies` twice.

### Database constraints

Unique ISBN/email constraints, foreign keys, and inventory checks are kept in SQLite because they protect persisted invariants even if application code changes.

### Scope

The service intentionally does not add Redis, messaging, JWT infrastructure, reservations, fines, or an external identity provider because none are required by the assignment.

## Known Limitation

The implementation is intended as a single-service assignment rather than a horizontally scaled lending platform. The conditional inventory decrement protects the last-copy update, but the active-loan-limit check and duplicate-return path are not designed as strict concurrency controls for simultaneous competing requests. If stronger concurrent or multi-instance guarantees become a requirement, those transaction boundaries should be revisited with database-level concurrency control appropriate to the production database.
