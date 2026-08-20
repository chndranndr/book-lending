# Book Lending Microservice

A self-contained Java microservice for managing a book lending system.

Repository: https://github.com/chndranndr/book-lending

## Running

```bash
docker compose up --build
```

The application is available at http://localhost:8080

## Stopping

```bash
docker compose down
```

## Resetting data

```bash
docker compose down -v
```

## Authentication

HTTP Basic authentication with predefined users:

| Username    | Password            | Role      |
|-------------|---------------------|-----------|
| admin       | admin-password      | ADMIN     |
| librarian   | librarian-password  | LIBRARIAN |

**ADMIN** can manage books, members, and loans.
**LIBRARIAN** can read books/members and borrow/return books.

## API Documentation

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## Configuration

| Key                               | Default | Description                        |
|-----------------------------------|---------|------------------------------------|
| `library.borrowing.max-active-loans`   | 3       | Maximum active loans per member    |
| `library.borrowing.loan-duration-days` | 14      | Loan duration in days              |

Override via environment variables:

```bash
LIBRARY_BORROWING_MAX_ACTIVE_LOANS=5
LIBRARY_BORROWING_LOAN_DURATION_DAYS=21
```

## Useful Endpoints

| Endpoint            | Description          |
|---------------------|----------------------|
| `/swagger-ui.html`  | Swagger UI           |
| `/actuator/health`  | Health check         |
| `/actuator/metrics` | Application metrics  |

## Running with Maven (optional)

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
