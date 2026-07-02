# Online Library Backend

Spring Boot 3.x REST API for a small online library with two user roles: **USER** (readers) and **ADMIN** (librarians). Authentication is HTTP Basic; all data lives in an H2 in-memory database.

## Tech Stack

| Layer       | Technology                              |
|-------------|----------------------------------------|
| Language    | Java 17                                 |
| Framework   | Spring Boot 3.3.4                       |
| Security    | Spring Security 6 (HTTP Basic, BCrypt) |
| Persistence | Spring Data JPA + H2 in-memory         |
| Validation  | Jakarta Bean Validation                 |
| Boilerplate | Lombok                                  |
| Build       | Maven                                   |

## Quick Start

```bash
mvn spring-boot:run
```

The app starts on **http://localhost:8080**.

H2 Console (dev only): **http://localhost:8080/h2-console**
- JDBC URL: `jdbc:h2:mem:onlinelib`
- User: `sa` · Password: *(empty)*

## Default Credentials

| Username | Password | Role       |
|----------|----------|------------|
| `admin`  | `admin`  | ROLE_ADMIN |

Created automatically on startup by `DataInitializer` if not present.

## Endpoints

| Method | Path         | Auth       | Access           | Description              |
|--------|--------------|------------|------------------|--------------------------|
| POST   | /register    | None       | Public           | Register a new reader    |
| GET    | /user/hello  | HTTP Basic | USER, ADMIN      | Greeting for readers     |
| GET    | /admin/hello | HTTP Basic | ADMIN only       | Greeting for librarians  |

## cURL Examples

### Register a new reader → 201 Created
```bash
curl -s -X POST http://localhost:8080/register \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "password": "secret123"}' | jq
```

### Access /user/hello as reader → 200 OK
```bash
curl -s -u alice:secret123 http://localhost:8080/user/hello
```

### Access /user/hello without credentials → 401 Unauthorized
```bash
curl -i http://localhost:8080/user/hello
```

### Reader tries /admin/hello → 403 Forbidden
```bash
curl -i -u alice:secret123 http://localhost:8080/admin/hello
```

### Admin accesses /admin/hello → 200 OK
```bash
curl -s -u admin:admin http://localhost:8080/admin/hello
```

### Register duplicate username → 409 Conflict
```bash
curl -s -X POST http://localhost:8080/register \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "password": "secret123"}' | jq
```

### Validation failure (short username/password) → 400 Bad Request
```bash
curl -s -X POST http://localhost:8080/register \
  -H "Content-Type: application/json" \
  -d '{"username": "ab", "password": "123"}' | jq
```

## Running Tests

```bash
mvn test
```

All tests including full build:
```bash
mvn clean install
```

## Architecture

### Why DTOs instead of returning Entities directly?

The `User` entity is a database model — it contains the hashed password and is tightly coupled to the schema. Returning it directly would:
- Leak the password hash in the JSON response
- Couple the API contract to the DB schema (a column rename breaks your API)
- Risk serialization problems with JPA lazy-loaded relations

`UserResponse` is a stable API contract independent of the DB. `RegisterRequest` is a validation boundary — annotations live there, not on the entity.

### Why a Service Layer?

Controllers should only handle HTTP concerns (parsing, status codes, routing). Business logic belongs in `UserService`:
- Duplicate username check
- Password hashing
- Role assignment

This makes business logic independently unit-testable without starting an HTTP server or touching a database (see `UserServiceTest`).

### Where does Spring Security check roles?

1. **`BasicAuthenticationFilter`** reads the `Authorization: Basic …` header, decodes credentials, and calls `UserDetailsServiceImpl.loadUserByUsername()` to load the user from the DB. It wraps the user in a `UsernamePasswordAuthenticationToken` and stores it in the `SecurityContextHolder`.

2. **`AuthorizationFilter`** (later in the chain) compares the `GrantedAuthority` list built from `user.role` (e.g. `ROLE_ADMIN`) against the rules declared in `SecurityFilterChain` — `hasRole("ADMIN")` checks for the `ROLE_ADMIN` authority.

3. Both steps happen **before** the request reaches any controller. For unauthorized or forbidden requests, Spring Security short-circuits and returns 401/403 directly — controllers are never invoked.

## Project Structure

```
src/main/java/com/example/onlinelib/
├── OnlineLibApplication.java
├── config/
│   └── DataInitializer.java      # Creates default admin on startup
├── controller/
│   ├── AuthController.java        # POST /register
│   ├── UserController.java        # GET /user/hello
│   └── AdminController.java       # GET /admin/hello
├── dto/
│   ├── RegisterRequest.java       # Validated input DTO
│   ├── UserResponse.java          # Safe output DTO (no password)
│   └── ErrorResponse.java         # Unified error format
├── entity/
│   └── User.java                  # JPA entity
├── exception/
│   ├── UsernameAlreadyExistsException.java
│   └── GlobalExceptionHandler.java
├── repository/
│   └── UserRepository.java
├── security/
│   ├── SecurityConfig.java        # BCrypt bean + SecurityFilterChain
│   └── UserDetailsServiceImpl.java
└── service/
    └── UserService.java           # Registration business logic
```
