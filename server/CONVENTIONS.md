# Back-end Conventions (controllers, services, repositories, domain models)

This document defines the naming, layering, and code-quality conventions for the
AutoDM Spring Boot back-end (`server/`). Every new or modified Java source file must
follow these conventions. Later tasks (for example, implementing controllers, services,
repositories, and domain models for feature work) are expected to reference and apply
this document.

## Layers and Their Responsibilities

The back-end uses a strict four-layer separation. A piece of code belongs to exactly one
layer. Responsibilities do not leak across layers.

| Layer            | Package hint        | Responsibility                                                        | Naming                                   |
|------------------|---------------------|-----------------------------------------------------------------------|------------------------------------------|
| Controller       | (root / `*Controller`) | Expose REST endpoints, validate input, delegate to a service.         | `NounController` (e.g. `MessageController`) |
| Service          | `service`           | Business logic, orchestration, rules, game logic.                     | `NounService` (e.g. `MessageService`)    |
| Repository       | `db`                | Data access / persistence only.                                       | `NounRepository` (e.g. `MessageRepository`) |
| Domain model     | (no layer restriction) | Plain data holders representing domain entities.                     | `Noun` (e.g. `Message`)                  |

### Controller

- Annotated with `@RestController` (or `@Controller` + `@ResponseBody`).
- Contains **only** HTTP concerns: mapping endpoints, reading request data, and
  returning response bodies.
- Must **not** contain business logic, data-access calls, or game logic. Any decision,
  calculation, validation beyond trivial binding, or state mutation belongs in a service.
- Thin by design: a method body should delegate to a service in one or a few calls.
- Never instantiate repositories or services as fields; inject them via constructor
  injection.

### Service

- Holds all business logic and game logic.
- Coordinates repositories and other services to fulfill a use case.
- Never contains HTTP annotations or returns Spring MVC types such as `ResponseEntity`
  purely to shape a response (it may return domain objects or service-level result types).
- Is the single place game rules, scoring, turn/state transitions, and other
  application behavior live.

### Repository

- Owns all persistence / data access.
- Exposes methods to fetch and persist domain models; contains no business logic.
- Uses a framework persistence abstraction (for example Spring Data) rather than
  hand-rolled SQL plumbing in controller or service code.

### Domain model

- Plain data classes describing domain entities.
- Prefer records or immutable holders for value-style models; use classes when behavior
  (methods) is required.
- Should not leak framework or persistence details into their shape.

## Naming Conventions

- Java types use `PascalCase`: `MessageController`, `MessageService`,
  `MessageRepository`, `Message`.
- Package names use `snake_case`-free lowercase dotted names, e.g. `com.example.service`.
- Method and variable names use `camelCase`: `findMessages`, `messageService`.
- REST paths use lowercase, hyphenated, noun-based segments, e.g. `/api/messages`.
- Name types by responsibility so the layer is obvious from the class name
  (`*Controller`, `*Service`, `*Repository`).

## Prohibited Java Declarations (JDK framework-type collisions)

Application code must **not** declare top-level or nested types whose names collide with
common JDK / framework types. Declaring your own `String`, `List`, and so on shadows the
platform types and is a serious quality and safety hazard.

Do **not** name any application type (class, interface, enum, record, type parameter)
after a common JDK or widely used framework type, including but not limited to:

`String`, `Object`, `Integer`, `Long`, `Double`, `Boolean`, `List`, `Set`, `Map`,
`Collection`, `Optional`, `Stream`, `Thread`, `Runnable`, `Exception`, `Error`,
`RuntimeException`, `Date`, `Calendar`, `Random`, `Scanner`, `InputStream`,
`OutputStream`, `Path`, `URI`, `URL`, `Duration`, `Instant`.

If you need collection-like or wrapper behavior, use the JDK type directly
(`java.util.List`, `java.lang.String`) rather than declaring your own.

## Code Quality

- Use constructor injection for all dependencies; avoid field injection.
- Keep classes focused and small. If a class grows beyond a single responsibility,
  extract a service or repository.
- Prefer immutable domain models.
- Keep imports ordered and unused imports removed.
- Do not hard-code platform-specific absolute paths or secrets; use configuration
  (for example `application.properties`).

## Locating Conventions

- This file: `server/CONVENTIONS.md`.
- Architecture context: `docs/architecture.md`.
- These conventions are referenced by later tasks; any new controller, service,
  repository, or domain model must comply with them.
