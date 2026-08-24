# Developer Setup

## Prerequisites

- Java 17 or newer
- Maven 3.8 or newer
- Node.js 18 or newer (with npm)

## Back-end (`server/`)

```bash
cd server
mvn spring-boot:run
```

The service listens on `http://localhost:5150`. Adjust settings in
`server/src/main/resources/application.properties`.

### SQLite database path

The database path is application-relative and configurable; no platform-specific
absolute path is hard-coded. By default the database file lives inside the user's
home directory and works the same on Windows, macOS and Linux:

```
${user.home}/autodm.db
```

Override the location by setting the `autodm.database.path` property, for example:

```bash
mvn spring-boot:run -Dautodm.database.path=./data/autodm.db
# or
export AUTODM_DATABASE_PATH=/tmp/autodm.db
```

## Front-end (`client/`)

```bash
cd client
npm install
npm start
```

The dev server listens on `http://localhost:5300`. Configure proxying to the back-end
in `client/proxy.conf.json`.

---

## Manual Acceptance Checklist

This checklist is intended to be run by hand after the autonomous build. Automated
testing is intentionally not part of this process. All ports below use the Argus-assigned
values (backend `5150`, frontend `5300`).

### 1. Back-end builds cleanly

```bash
cd server
./mvnw -DskipTests package
```

Expected: `BUILD SUCCESS` and a jar named `target/autodm-server-*.jar` is produced.

### 2. Front-end builds in production mode

```bash
cd client
npm install
npm run build
```

Expected: `Application bundle generation complete` and an output location under
`client/dist/client`. Style-budget *warnings* are acceptable; the build must not fail.

### 3. Back-end starts and initializes the SQLite database safely

```bash
cd server
./mvnw spring-boot:run
```

Expected:

- `Tomcat started on port 5150` and `Started Application` appear in the log.
- On a clean start (no existing database), the database directory is created and the
  schema is applied idempotently. On restart, existing data is left untouched.
- `GET http://localhost:5150/health` returns a healthy status.

### 4. Front-end starts and talks to the back-end

```bash
cd client
npm start
```

Expected: `ng serve` reports the dev server on `http://localhost:5300`. Opening that URL
in a browser loads the app, and API calls are proxied to the back-end on port `5150`
(without CORS errors). The dashboard/campaigns UI loads without console errors.

### 5. End-to-end smoke test

- In the UI (or with curl), create a campaign:

  ```bash
  curl -X POST http://localhost:5150/api/campaign-management \
       -H "Content-Type: application/json" \
       -d '{"title":"Acceptance Test"}'
  ```

  Expected: a JSON campaign object with a populated `id` and `status` (e.g. `DRAFT`).
- Reload the front-end and confirm the created campaign appears.

### 6. Ports confirmed

- Back-end: `5150` (set in `server/src/main/resources/application.properties`).
- Front-end: `5300` (set in `client/package.json` `npm start` and
  `client/proxy.conf.json`).
- No default ports (3000, 4200, 5000, 5173, 8000, 8080, 8081, 8888) are used.

