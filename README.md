# PrintDoc — full-stack prototype

PrintDoc is a responsive QR-led print-shop workflow prototype with a separate Java/Spring Boot API. The upstream repository was cloned and inspected, but the original service's proprietary backend was not present. This implementation is a new, deliberately constrained API—not a drop-in clone of the unknown remote service. Spring Boot `4.1.2` and Springdoc `3.2.0` were checked against Maven Central release metadata as the latest stable versions during implementation; both are recent releases and should be revisited for long-term support policy.

## What the requested repository contains

Static inspection of `itgyaan/scanandprint` found a prebuilt React SPA in `app/dist`, a Node static file server/proxy in `server.js`, and reverse-engineered Electron desktop-agent source in `agent_source_code`. The Node server serves the SPA, returns fixed mock admin data, and forwards other `/api/*` traffic to a hard-coded hosted API. The bundle names public marketing, kiosk, photo/document editor, owner console, and admin console components. The agent source advertises authenticated WebSocket registration, printer discovery, queue events, and local print execution.

The repository is not the actual backend implementation: it contains mock fixtures and a remote proxy. Its README/deployment guide includes plaintext demonstration credentials and identifiable shop/contact records; those were deliberately not copied, tested, repeated, or used. The remote API's persistence schema, production authentication rules, payment flow, queue guarantees, and full request contracts could not be verified. Minified public bundle strings and static code are evidence of UI concepts, not proof of production server behavior.

### Safe assumptions

- Shop account, quote, job, and plan catalog are the core data concepts.
- Public kiosk requests are anonymous and shop-scoped; owner profile/job reads require bearer JWT authentication.
- Uploaded documents are temporary, stored under generated keys outside the public static tree, size/type checked, hashed, and scheduled for deletion.
- Price fields are integer minor units (for INR paise) to avoid floating point money errors.
- Cash approval can create a demonstration queue state; UPI is represented as awaiting payment. No payment gateway is present.
- No real printer control, Windows agent protocol, Socket.IO commands, file downloads, payment capture, or admin panel was integrated. These features require separate threat modeling and server-side/device authentication.

## Project structure

```text
ScanPrint_Job/
├── README.md
├── package.json                  # React/Vite dev/build and optional full-stack runner
├── .env.example                  # Frontend API URL and explicit opt-in
├── index.html
├── src/
│   ├── main.jsx                  # Public routes and page components
│   ├── api.js                    # Opt-in API client; session-only JWT
│   ├── OwnerPortal.jsx           # Owner dashboard routes
│   ├── style.css                 # Core responsive interface
│   ├── studio-dashboard.css
│   ├── owner-portal.css
│   ├── safety.css
│   ├── extras.css
│   └── home-extras.css
└── backend/
    ├── pom.xml
    ├── mvnw.cmd                  # Maven wrapper (Maven 3.9.9)
    ├── .mvn/wrapper/             # Wrapper JAR + distribution URL
    └── src/
        ├── main/java/com/printdoc/api/
        │   ├── PrintDocApplication.java
        │   ├── auth/              # Login/register DTO, JWT service/filter/controller
        │   ├── common/            # Standard API response, exceptions, current user
        │   ├── config/            # Security, OpenAPI, seed, cleanup
        │   ├── job/               # Temporary uploads, print jobs, DTO, service, controller
        │   ├── plan/              # Plan entity/repository/catalog service/controller
        │   └── shop/              # Shop/account entities, repositories, profile service/controller
        ├── main/resources/
        │   ├── application.properties
        │   ├── application-dev.properties
        │   ├── application-prod.properties
        │   └── db/migration/V1__create_core_tables.sql
        └── test/java/com/printdoc/api/
            ├── PrintDocApplicationTests.java
            ├── auth/JwtServiceTest.java
            └── job/JobServiceTest.java
```

## Architecture and data flow

- **Controller**: accepts HTTP requests, validates inputs, returns `ApiResponse<T>` JSON envelopes.
- **DTO**: request/response records prevent database entities from becoming the API contract.
- **Service**: applies shop scope, validates state transitions, calculates prices, hashes and stores uploads.
- **Repository/entity**: Spring Data JPA repositories persist shops, users, plans, upload metadata, jobs and event history.
- **Security**: stateless Spring Security; BCrypt password hashes; HMAC JWT access tokens; allowlisted CORS origins; owner endpoints require role authentication. JWT secret is mandatory and must not be committed. Bootstrap is disabled unless `BOOTSTRAP_ADMIN_ENABLED=true` plus a high-entropy secret password are explicitly supplied.
- **Database**: file-backed H2 for local development; PostgreSQL URL/credentials plus Flyway migration for production.
- **OpenAPI**: Swagger UI at `/swagger-ui.html`, JSON document at `/api-docs`.
- **Temporary file storage**: upload bytes go into `./data/uploads`, outside the static `src`/`dist` folders. Metadata contains only opaque IDs, sanitized names, MIME, size and hash. Expired upload rows/files are cleaned on a timer. The original filename field is not used as a path.

### Deliberate safety limits

The default frontend sends **no** API requests: `VITE_API_ENABLED=false`. When explicitly enabled, API calls go only to `VITE_API_BASE_URL`, defaulting to `http://localhost:8080`, with cookies omitted from fetch. The shop-owner JWT is stored in `sessionStorage` and is cleared when that tab's session ends; this is appropriate only for this local demo, not a hardened production browser session. The UI shows the selected URL. No contact form submission, email/SMS, card/UPI collection, external proxy, analytics, original company contact information, production owner credentials, or printer command is configured. Do not enable API access against a server you do not control. Never paste credentials listed in the upstream repo README.

The API stores temporary upload bytes and local sample records; it does not analyze PDF contents. Page count is estimated from byte size except image files, which count as one page. The API returns `WAITING_COUNTER_APPROVAL` for cash and `AWAITING_PAYMENT` for UPI; neither state triggers a real collection or print action.

## REST API reference

Base path: `http://localhost:8080/api/v1`. Success shape is `{ "success": true, "message": "...", "data": ..., "timestamp": "..." }`. Errors use the same envelope with `success:false`; validation errors place field messages in `data`.

| Method | Endpoint | Auth | Purpose |
|---|---|---|---|
| `POST` | `/auth/shop/register` | Public (flag-gated) | Create a shop and owner account, then return a bearer JWT. Requires `printdoc.auth.registration-enabled=true` (`ALLOW_PUBLIC_REGISTRATION`), otherwise `403`. |
| `POST` | `/auth/shop/login` | Public | Authenticate email/password and return a short-lived bearer JWT. |
| `GET` | `/auth/me` | JWT | Return the current account and shop. |
| `GET` | `/public/shops/{shopCode}` | Public | Read public shop label and sample per-page rates. |
| `GET` | `/public/plans` | Public | Read the active plan catalog. Prices/limits are samples; no billing. |
| `POST` | `/public/shops/{shopCode}/uploads` | Public | Multipart field `file`; type and size allowlist; temporary upload. |
| `POST` | `/public/shops/{shopCode}/jobs` | Public | Create an idempotent print quote/job using `uploadIds`, `pageRange`, `copies`, `colorMode`, `duplexMode`, `paperSize`, and `paymentMethod`. API computes amount. |
| `GET` | `/public/jobs/{jobId}/status` | Public | Read the opaque, short-retention sample job status. |
| `GET` | `/owner/profile` | Owner JWT | Read owner shop/profile and sample rates. |
| `PUT` | `/owner/profile` | Owner JWT | Update display name and B&W/color/duplex rates (minor units). |
| `GET` | `/owner/jobs?page=0&size=20&status=WAITING_COUNTER_APPROVAL` | Owner JWT | Paginated jobs, scoped to authenticated shop. |
| `GET` | `/owner/jobs/{jobId}` | Owner JWT | Read a shop-scoped job. |
| `POST` | `/owner/jobs/{jobId}/transition` | Owner JWT | Update permitted demo states; UPI jobs cannot be approved because no gateway is integrated. |
| `GET` | `/owner/setup-status` | Owner JWT | Report readiness and blocking gaps for all eight setup steps. |
| `GET` | `/devices/check-status?shopCode=...&fingerprint=...` | Public, rate-limited | Create/find a pending agent device request and report approval status. The legacy `/api/devices/check-status` path is also accepted. |
| `GET` | `/owner/devices` | Owner JWT | List devices belonging to the authenticated shop. `/devices/my-devices` is also accepted. |
| `POST` | `/owner/devices/credentials` | Owner JWT | Rotate and return a one-time desktop-agent secret; only its BCrypt hash is stored. |
| `POST` | `/owner/devices/{deviceId}/approve` | Owner JWT | Approve a pending device. Reject and revoke actions are also available. |
| `POST` | `/agent/register` | Agent secret + approved fingerprint | Register agent metadata and mark the device online. |
| `POST` | `/agent/heartbeat` | Agent secret + approved fingerprint | Refresh the device heartbeat and online status. |
| `GET` | `/actuator/health` | Public | Health check. |
| `GET` | `/swagger-ui.html` | Public | Interactive API docs. |
| `GET` | `/api-docs` | Public | OpenAPI JSON. |

Status lifecycle is intentionally limited to `AWAITING_PAYMENT`, `WAITING_COUNTER_APPROVAL`, `QUEUED`, `PRINTING`, `COMPLETED`, `CANCELLED`, and `FAILED`. In this codebase, no agent endpoint transitions into printer execution; don't treat these states as evidence that a device is connected.

### Socket.IO agent gateway

Set `SOCKET_IO_ENABLED=true` to start the compatible Socket.IO v4 gateway. It listens on `SOCKET_IO_HOSTNAME`/`SOCKET_IO_PORT` (default `0.0.0.0:9092`) at `/socket.io`. The gateway authenticates the agent `auth` object using `shopId`/`secretKey`/`deviceFingerprint`, isolates clients into `shop:{SHOP_CODE}` rooms, and supports `AGENT_REGISTER`, `AGENT_HEARTBEAT`, `JOB_SUCCESS`, `JOB_FAILED`, `AGENT_PRINTERS_UPDATED`, and `AGENT_CONFIG_UPDATE_ACK`. It emits `AGENT_CONNECTED`, `JOB_STATUS_UPDATED`, `PRINT_JOB_DISPATCH`, `CANCEL_HELD_JOB`, and `REQUEST_PRINTER_SCAN`.

Because the Spring REST API uses port 8080 and the Socket.IO gateway uses a separate Netty listener, production deployments must either expose a separate agent socket URL or route `/socket.io` through a reverse proxy to the configured socket port. Do not expose the development wildcard origin in production; set `SOCKET_IO_ALLOWED_ORIGIN` to the controlled origin.

## Setup and run (Windows PowerShell)

### Optional Docker Compose (production-like stack)

Install Docker Desktop, open a terminal at `backend`, then set **new secret values** and start the Postgres/API containers:

```powershell
$env:DATABASE_PASSWORD = 'choose-a-long-random-database-password'
$env:JWT_SECRET = 'choose-a-different-random-string-at-least-32-characters-long'
$env:CORS_ALLOWED_ORIGINS = 'http://localhost:5173'
docker compose up --build
```

Do not use the example passwords in production. The API container listens on `http://localhost:8080`; PostgreSQL data is kept in a Docker volume. Stop with `docker compose down` (to delete DB data use `docker compose down -v`). Production profile requires PostgreSQL env vars, Flyway migration, a secure JWT key, and an origin allowlist.

Requirements: Java 17 or newer, Node.js 20+ (tested with 24), internet access on first Maven dependency download. Maven is included by the wrapper; global `mvn` is not needed.

### 1. Start the backend

```powershell
cd backend
$bytes = New-Object byte[] 48
[Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
$env:SOCKET_IO_ENABLED = 'true'
$env:SOCKET_IO_PORT = '9092'
cmd /c mvnw.cmd test
# Or from the project root, run `npm run api` to create a temporary local JWT key for this process.
cmd /c mvnw.cmd spring-boot:run
# or, from the repository root: npm run api (temporary local JWT secret)
```

The API starts at `http://localhost:8080`. The local database file is under `backend/data/printdoc.mv.db`. Temporary uploads are in `backend/data/uploads` and expire after two hours; never store confidential documents in this prototype. `GET /actuator/health` should report `UP`. On an existing local database, manually inspect and add the synthetic shop code `PRINTDOC_DEMO` before using the kiosk; the backend does not silently upgrade an old data file.

Optional local owner bootstrap (choose your own test account; do not reuse a real password):

```powershell
$env:BOOTSTRAP_ADMIN_EMAIL = 'owner@example.invalid'
$env:BOOTSTRAP_ADMIN_PASSWORD = 'use-a-new-local-test-password-at-least-12-chars'
$env:BOOTSTRAP_ADMIN_ENABLED = 'true'
$env:JWT_SECRET = 'replace-with-a-generated-random-string-of-at-least-32-characters'
cmd /c mvnw.cmd spring-boot:run
```

If first run already populated the database, delete `backend/data/printdoc.mv.db` **only when you are comfortable erasing local sample data**, then restart to bootstrap. Bootstrap account creation only happens when explicitly enabled and configured; use a password manager or secret manager for that password, not the placeholder above. With the `dev` profile you can instead create your own account from the signup screen.

### 2. Start the frontend in local-preview mode

```powershell
cd ..
npm install
npm run dev:fullstack
```

The combined script starts Vite and the Spring API together. For stricter process isolation, use two terminals: run `npm run api` in one and `npm run dev` in another. Open the Vite URL (usually `http://127.0.0.1:5173`). API access remains disabled by default; use the UI as a local mockup. The Node frontend server is separate from the Java API. For one-command local development, run `npm run dev:fullstack` from the root; it creates a random temporary JWT signing key and starts both Vite and the API. The temporary key is lost on process restart; do not use this convenience script as a production launcher.

### 3. Create an account and sign in (signup)

Account creation is now implemented and gated by a server-side flag:

| Setting | Where | Default | Effect |
|---|---|---|---|
| `VITE_API_ENABLED` | frontend `.env.local` | `false` | When `false` the UI sends **no** requests and shows "API disabled". |
| `ALLOW_PUBLIC_REGISTRATION` | backend env / profile | `true` in `dev`, `false` in `prod` | Allows `POST /api/v1/auth/shop/register`. |

Local setup (already applied to `.env.local` in this workspace):

```dotenv
VITE_API_ENABLED=true
VITE_API_BASE_URL=http://localhost:8080
```

Then start both processes and use the signup screen:

```powershell
npm run dev:fullstack      # Vite + API with a temporary local JWT key
```

Open `http://127.0.0.1:5173/register`, fill in shop name, owner name, email, password, confirm password, and optionally a shop code (leave blank and the server derives a unique one such as `PRINTDOCSIGN_8127`). The API also accepts optional `phone` and `printerBrand` fields and records a 48-hour trial window. Successful signup creates the shop plus an `OWNER` account, returns a JWT, and takes you straight to the owner dashboard. Duplicate email → `409`, weak/short password → `400`, registration disabled on the server → `403` with instructions.

Registration rules: password 8–128 characters, shop code matches `[A-Za-z0-9_-]{4,32}` and must be unique, email must be unique and syntactically valid. Passwords are stored as BCrypt hashes; tokens are kept in `sessionStorage` and expire after 8 hours. Never reuse a password from another service, and never use credentials found in the upstream repository.

To provision accounts without public signup (recommended for production), keep `ALLOW_PUBLIC_REGISTRATION=false` and use the bootstrap environment variables or an administrator-only flow.

### 4. Use the owner portal, kiosk, and API mode

Restart Vite, go to **Shop login**, and enter the local account you created. The owner portal then reads its own shop's job page. On the kiosk screen, select a local file; it is uploaded only to the configured API, then you may create a cash-at-counter sample job. **No payment is collected and no document is sent to a printer.** Never set the URL to the original company or any server you do not control. After testing, set `VITE_API_ENABLED=false` again.

### 4. Tests and production profile

```powershell
cd backend
cmd /c mvnw.cmd test
cmd /c mvnw.cmd package
```

Production uses PostgreSQL and Flyway migration:

```powershell
$env:SPRING_PROFILES_ACTIVE='prod'
$env:DATABASE_URL='jdbc:postgresql://localhost:5432/printdoc'
$env:DATABASE_USERNAME='printdoc_app'
$env:DATABASE_PASSWORD='use-a-secret-manager-value'
$env:JWT_SECRET='a-random-secret-at-least-32-characters-long'
$env:CORS_ALLOWED_ORIGINS='https://your-own-frontend.example'
$env:PRINTDOC_SEED_ENABLED='false'
cmd /c mvnw.cmd spring-boot:run
```

Before deployment add TLS termination, backups, distributed rate limiting, audit retention policy, key rotation, dependency scanning, upload malware scanning, quota controls, and an authorization review. Phase 1 now implements device enrollment/approval, one-time agent-secret rotation, authenticated REST registration, heartbeats, and a separately configured Socket.IO v4 gateway. Printer persistence/discovery, payment webhooks, refresh-token revocation, and secure document download/spooling remain later phases.

## What changed and why

1. **Audited before designing**: found the requested repository's `server.js` is a static-file host plus mock admin API and unrestricted remote proxy, not the source of its production backend. The proprietary hosted backend cannot be verified.
2. **Replaced proxy assumptions with a new `/api/v1` contract**: no remote forwarding, origin spoofing, static admin token, entitlement rewriting, or real-contact fixtures are copied.
3. **Added relational data model and validation**: shop/accounts, plan catalog, temporary file metadata, print jobs and append-only state events.
4. **Added independent API security**: BCrypt hashes, explicit CORS list, stateless JWT, per-shop job access, consistent error envelopes.
5. **Added safe kiosk workflow**: MIME/size checks, normalized filenames, opaque storage keys, SHA-256, short expiry, server-side quote calculation, idempotency keys.
6. **Made real API opt-in on the frontend**: no request until explicitly enabled. API target is shown; frontend fetch omits cookies/credentials.
7. **Kept payment/printing disconnected**: a quote/job isn't payment evidence and doesn't execute a hardware side effect.
8. **Added Maven wrapper, profiles, OpenAPI, tests, and local-first docs**.

## Known gaps and important caveats

- Spring Boot backend models only shop auth/profile, sample plans, temporary uploads, and simple print jobs. It is not a full production payment or printer backend.
- The source repository's remote APIs were not called; bundled frontend had minified/opaque requests. Proposed API endpoints are clearly new interfaces, not verified upstream contracts.
- DTO `CreateRequest` is for one requested print variant but can accept several uploaded documents; the stored job currently references the first document's filename/hash, while total byte size/pages are aggregated. For multi-document production jobs, introduce a `PrintJobFile` join entity and line-item quote structure before enabling multi-upload.
- Estimated page count is heuristic. Integrate a hardened document parser, page-count ceiling, antivirus/sandbox, and encrypted object store before real document service.
- API does not store uploaded bytes permanently, but the local filesystem temporary content persists until its expiry cleanup. Delete local `backend/data` after experiments if appropriate.
- Registration is implemented but flag-gated and intentionally minimal: no email verification, CAPTCHA, rate-limit tuning per account, or password reset. Keep `ALLOW_PUBLIC_REGISTRATION=false` publicly until those controls exist; use bootstrap/admin provisioning instead.
- We did not reproduce any of the original repository's private credentials, personal shop data, or contact details.
