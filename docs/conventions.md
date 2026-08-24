# Conventions

This directory collects the conventions that govern how the AutoDM code is
organized. Every new or modified source file across the project should follow the
conventions that apply to its layer.

## Back-end (Spring Boot / Java)

Naming, layering, and code-quality conventions for the Spring Boot back-end are
defined in:

- `server/CONVENTIONS.md`

That document covers:

- The separation between **controllers**, **services**, **repositories**, and
  **domain models**, and which responsibilities belong to each layer.
- The rule that application code must **not** declare types that collide with
  common JDK / framework types (for example `String`, `List`, `Map`).
- The requirement that **controllers stay thin** and that all **game logic and
  business logic live in services**, never in controllers.

Later tasks that implement controllers, services, repositories, or domain models
must reference and comply with `server/CONVENTIONS.md`.
