# Online Library Backend

<p align="left">
  <img src="https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring%20Boot-3.3.4-6DB33F?style=flat-square&logo=springboot&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring%20Security-6-6DB33F?style=flat-square&logo=springsecurity&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring%20Data%20JPA-6DB33F?style=flat-square&logo=spring&logoColor=white" />
  <img src="https://img.shields.io/badge/H2-in--memory-4479A1?style=flat-square&logo=h2&logoColor=white" />
  <img src="https://img.shields.io/badge/Maven-C71A36?style=flat-square&logo=apachemaven&logoColor=white" />
  <img src="https://img.shields.io/badge/Lombok-BC4521?style=flat-square&logo=lombok&logoColor=white" />
</p>

> **Бэкенд небольшой онлайн-библиотеки** на Spring Boot 3.x с ролевой моделью доступа и базовой аутентификацией.

Проект реализует REST API для системы, в которой есть два типа пользователей:
- **USER (читатель)** — может просматривать каталог и брать книги
- **ADMIN (библиотекарь)** — управляет фондом и пользователями

Аутентификация построена на HTTP Basic Auth с хранением паролей в BCrypt. Все данные хранятся в H2 in-memory базе — никаких внешних зависимостей для запуска не нужно.


## Быстрый старт

```bash
mvn spring-boot:run
```

Приложение запускается на **http://localhost:8080**.

H2 Console (только для разработки): **http://localhost:8080/h2-console**
- JDBC URL: `jdbc:h2:mem:onlinelib`
- User: `sa` · Password: *(пусто)*

## Учётные данные по умолчанию

| Пользователь | Пароль | Роль       |
|--------------|--------|------------|
| `admin`      | `admin`| ROLE_ADMIN |

Создаётся автоматически при старте через `DataInitializer`, если пользователь ещё не существует.

## Эндпоинты

| Метод | Путь         | Авторизация | Доступ           | Описание                       |
|-------|--------------|-------------|------------------|--------------------------------|
| POST  | /register    | Нет         | Публичный        | Регистрация нового читателя    |
| GET   | /user/hello  | HTTP Basic  | USER, ADMIN      | Приветствие для читателей      |
| GET   | /admin/hello | HTTP Basic  | Только ADMIN     | Приветствие для библиотекарей  |

## Примеры cURL

### Регистрация нового читателя → 201 Created
```bash
curl -s -X POST http://localhost:8080/register \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "password": "secret123"}' | jq
```

### Запрос /user/hello с авторизацией → 200 OK
```bash
curl -s -u alice:secret123 http://localhost:8080/user/hello
```

### Запрос /user/hello без авторизации → 401 Unauthorized
```bash
curl -i http://localhost:8080/user/hello
```

### Читатель пробует /admin/hello → 403 Forbidden
```bash
curl -i -u alice:secret123 http://localhost:8080/admin/hello
```

### Админ заходит в /admin/hello → 200 OK
```bash
curl -s -u admin:admin http://localhost:8080/admin/hello
```

### Регистрация с занятым username → 409 Conflict
```bash
curl -s -X POST http://localhost:8080/register \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "password": "secret123"}' | jq
```

### Ошибка валидации (короткий username/пароль) → 400 Bad Request
```bash
curl -s -X POST http://localhost:8080/register \
  -H "Content-Type: application/json" \
  -d '{"username": "ab", "password": "123"}' | jq
```

## Запуск тестов

```bash
mvn test
```

Полная сборка со всеми тестами:
```bash
mvn clean install
```

## Архитектура

### Почему DTO, а не Entity наружу?

`User` — это модель базы данных, которая содержит хеш пароля и жёстко привязана к схеме. Возвращать её напрямую значит:
- Отдать хеш пароля в JSON-ответе
- Привязать API-контракт к схеме БД (переименование колонки ломает API)
- Получить проблемы с сериализацией JPA lazy-loaded отношений

`UserResponse` — стабильный API-контракт, независимый от БД. `RegisterRequest` — граница валидации: аннотации живут на DTO, а не на Entity.

### Почему Service Layer?

Контроллер занимается только HTTP-транспортом (парсинг, статус-коды, маршрутизация). Бизнес-логика — в `UserService`:
- Проверка занятости username
- Хеширование пароля
- Назначение роли

Это позволяет тестировать бизнес-логику юнит-тестами без поднятия HTTP-сервера и обращения к БД (см. `UserServiceTest`).

### Где Spring Security проверяет роль?

1. **`BasicAuthenticationFilter`** читает заголовок `Authorization: Basic …`, декодирует учётные данные и вызывает `UserDetailsServiceImpl.loadUserByUsername()` для загрузки пользователя из БД. Оборачивает его в `UsernamePasswordAuthenticationToken` с authorities и кладёт в `SecurityContextHolder`.

2. **`AuthorizationFilter`** (позже в цепочке) сверяет список `GrantedAuthority` из токена с правилами `SecurityFilterChain` — `hasRole("ADMIN")` ищет authority `ROLE_ADMIN`.

3. Оба шага происходят **до контроллера**. При 401/403 Spring Security отвечает сам — контроллеры не вызываются вообще.

## Структура проекта

```
src/main/java/com/example/onlinelib/
├── OnlineLibApplication.java
├── config/
│   └── DataInitializer.java          # Создаёт admin при старте
├── controller/
│   ├── AuthController.java            # POST /register
│   ├── UserController.java            # GET /user/hello
│   └── AdminController.java           # GET /admin/hello
├── dto/
│   ├── RegisterRequest.java           # Входной DTO с валидацией
│   ├── UserResponse.java              # Выходной DTO (без пароля)
│   └── ErrorResponse.java             # Единый формат ошибок
├── entity/
│   └── User.java                      # JPA-сущность
├── exception/
│   ├── UsernameAlreadyExistsException.java
│   └── GlobalExceptionHandler.java
├── repository/
│   └── UserRepository.java
├── security/
│   ├── SecurityConfig.java            # BCrypt бин + SecurityFilterChain
│   └── UserDetailsServiceImpl.java
└── service/
    └── UserService.java               # Бизнес-логика регистрации
```
