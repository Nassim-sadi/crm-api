# CRM Ticket Management API

A RESTful CRM ticket-management API built with Spring Boot. Supports customers, employees, tickets (with a strict status workflow), comments, and JWT-based authentication.

## Tech stack

- **Java 26** / **Spring Boot 4.1.0**
- Spring Web (MVC), Spring Data JPA (Hibernate), Bean Validation
- Spring Security 7 + **JJWT 0.12** (stateless JWT auth, BCrypt password hashing)
- springdoc-openapi 3.1 (Swagger UI at `/swagger-ui/index.html`)
- H2 in-memory database (no external DB required)
- Maven wrapper (`./mvnw`), Maven 3.9.16

## Quick start

### 1. Run the tests

```bash
./mvnw test
```

### 2. Run the application

```bash
./mvnw spring-boot:run
# or
./mvnw package -DskipTests
java -jar target/crm-api-0.0.1-SNAPSHOT.jar
```

### 3. Run with Docker

```bash
docker compose up --build
```

The API is available at `http://localhost:8080`. The H2 console is enabled at `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:crmdb`, user `sa`, empty password).

## API documentation (Swagger UI)

Interactive OpenAPI documentation is available without authentication:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- OpenAPI YAML: `http://localhost:8080/v3/api-docs.yaml`

Click **Authorize** in Swagger UI and paste a JWT (`Bearer <token>`) from `/api/auth/login` to try authenticated endpoints directly.

## Authentication

All endpoints except `/api/auth/**` require a `Bearer` token.

1. Log in with the seeded administrator (created automatically on startup):

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@crm.com","password":"admin123"}'
```

2. Use the returned `token` on every request:

```bash
curl -s http://localhost:8080/api/customers \
  -H "Authorization: Bearer <token>"
```

3. Or register a new employee:

```bash
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Ada Lovelace","email":"ada@crm.com","password":"secret123","role":"AGENT"}'
```

- Tokens are HS256-signed, expire after 24h, and carry the user's `role`.
- Roles: `ADMIN`, `MANAGER`, `AGENT`, `SUPPORT`. `/api/employees/**` requires `ADMIN` or `MANAGER`; the rest requires any authenticated user.

## Endpoints

### Auth

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | public | Register an employee, returns `{token, employee}` |
| POST | `/api/auth/login` | public | Login, returns `{token, employee}` |

### Customers

| Method | Path | Description |
| --- | --- | --- |
| GET | `/api/customers` | List all customers |
| GET | `/api/customers/{id}` | Get one customer |
| POST | `/api/customers` | Create (201 + `Location`) |
| PUT | `/api/customers/{id}` | Update |
| DELETE | `/api/customers/{id}` | Delete (204) |

### Employees (ADMIN / MANAGER only)

| Method | Path | Description |
| --- | --- | --- |
| GET | `/api/employees` | List all employees |
| GET | `/api/employees/{id}` | Get one employee |
| POST | `/api/employees` | Create (optional `password`) |
| PUT | `/api/employees/{id}` | Update |
| DELETE | `/api/employees/{id}` | Delete (204) |

### Tickets

| Method | Path | Description |
| --- | --- | --- |
| GET | `/api/tickets?status=&priority=&customer=&assignedEmployee=&page=&size=` | Search + paged list |
| GET | `/api/tickets/{id}` | Get one ticket |
| POST | `/api/tickets` | Create (201 + `Location`) |
| PUT | `/api/tickets/{id}` | Update |
| PUT | `/api/tickets/{id}/assign` | Assign an employee |
| POST | `/api/tickets/{id}/start` | `OPEN` -> `IN_PROGRESS` |
| POST | `/api/tickets/{id}/resolve` | `IN_PROGRESS` -> `RESOLVED` |
| POST | `/api/tickets/{id}/close` | `IN_PROGRESS`/`RESOLVED` -> `CLOSED` |
| POST | `/api/tickets/{id}/reopen` | `CLOSED`/`RESOLVED` -> `OPEN` |
| DELETE | `/api/tickets/{id}` | Delete (204) |

Status workflow (strict, enforced by the service):

```
OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED
  ^                      |            |
  +----------------------+---- CLOSED +
```

### Comments

| Method | Path | Description |
| --- | --- | --- |
| GET | `/api/tickets/{ticketId}/comments` | List comments for a ticket |
| POST | `/api/tickets/{ticketId}/comments` | Add a comment (201 + `Location`) |

## Error format

All errors use a consistent JSON shape:

```json
{
  "status": 400,
  "message": "Invalid request",
  "errors": {
    "email": "email must be a valid email address"
  }
}
```

Common status codes: `400` validation / bad request, `401` missing or invalid token, `403` insufficient role, `404` not found, `409` duplicate email, `500` unexpected error.

## Oracle / PL-SQL demo

The `database/oracle/` folder contains an Oracle DDL schema and a PL/SQL package demonstrating stored procedures, functions, cursors, custom exceptions, and packages. See [`database/oracle/README.md`](database/oracle/README.md).

## Project structure

```
src/main/java/com/nassim/crm_api/
  auth/          JWT register/login
  security/      JWT service, filter, security config
  config/        OpenAPI config, data seeding (admin user)
  Customer/      customer domain + API
  Employee/      employee domain + API
  Ticket/        ticket domain, status workflow, API
  Comment/       ticket comments + API
  exception/     custom exceptions + global handler
database/oracle/ Oracle DDL + PL/SQL demo
```
