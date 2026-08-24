# AutoDM

AutoDM is a full-stack application for automated direct messaging. It consists of an
Angular single-page front-end and a Spring Boot REST back-end, plus shared documentation
in the top-level `docs/` directory.

## Technology Stack

| Layer        | Technology                        |
|--------------|-----------------------------------|
| Front-end    | Angular (see `client/`)           |
| Back-end     | Spring Boot (see `server/`)       |
| Documentation| Markdown in `docs/`               |

## Repository Layout

```
.
├── client/      Angular application (frontend)
├── server/      Spring Boot application (backend)
├── docs/        Shared documentation
└── README.md    This file
```

There is intentionally no project-name nesting: each application's files live directly
under `client/` and `server/`.

## Prerequisites

- Java 17+ (for the Spring Boot back-end)
- Maven 3.8+ (for the back-end build)
- Node.js 18+ and npm (for the Angular front-end)

## Running the Backend Locally

From the repository root (or from inside `server/`):

```bash
cd server
mvn spring-boot:run
```

The API is served on `http://localhost:5150`. Configure the port and other settings
in `server/src/main/resources/application.properties`.

## Running the Frontend Locally

From the `client/` directory:

```bash
cd client
npm install
npm start
```

Then open http://localhost:5300 in your browser. The development server proxies API
requests to the back-end; adjust the proxy configuration in `client/proxy.conf.json` as
needed.

## Documentation

See the `docs/` directory for architecture, API, and setup documentation shared across
both applications.
