# PublicCare backend

Spring Boot 3.5 backend for PublicCare. Java 21 and PostgreSQL are required.

## Local setup

Configure `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` in the process environment, or create `backend/.env` from `.env.example` and replace the placeholders locally. Never commit credentials. The default URL points to `jdbc:postgresql://localhost:5432/publiccare_db`; PostgreSQL database credentials are required. Start Maven with `backend` as the working directory so the optional `.env` import resolves correctly.

Flyway applies `src/main/resources/db/migration/V1__create_users.sql` through `V5__add_public_report_approvals.sql` on startup. For a fresh database, Flyway creates the schema. Existing databases that already have V1 and V2 applied are baselined at version 2 and receive V3 through V5; databases already migrated through V3 receive V4 and V5. Back up the database before applying schema changes. Hibernate remains configured with `ddl-auto: none`; schema changes are made only by Flyway migrations. The regular application startup never provisions an administrator. For a local development database, use the one-time command below.

The frontend origin is restricted to `http://localhost:5173`. Cookies are HTTP-only for the session and same-site `Lax`. For HTTPS deployments set `SESSION_COOKIE_SECURE=true`; leave it false only for local HTTP development. Keep database and cookie settings in environment variables, not source control.

Report images are limited to one JPG, PNG, or WebP file of at most 5 MiB per report. Files are stored outside the application and web roots in `${user.home}/.publiccare/uploads` by default; set `PUBLICCARE_UPLOAD_DIR` to choose another private directory. The database stores only a generated storage key, verified media type, file size, and report relationship. Keep this directory private and include it in the application's protected backup/restore process.

Administrator progress updates are retained as separate rows in `report_progress_updates`, in chronological order, with status, optional note, timestamp, and optional JPG/PNG photo metadata. Each update can include one JPG or PNG image up to 5 MiB, stored in the same private upload directory with a generated storage key. Status, note, and optional image are submitted together, so an invalid image cannot leave behind a partial status update. Citizen timeline and image endpoints enforce report ownership on the backend.

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/publiccare_db'
$env:DB_USERNAME = 'your_postgres_username'
$env:DB_PASSWORD = 'your_local_password'
./mvnw.cmd spring-boot:run
```

### Create the first local administrator

Run this only against a local PostgreSQL database (`DB_URL` must use `localhost`, `127.0.0.1`, or `::1`). It is an explicit, non-web setup command and requires `PUBLICCARE_ADMIN_SETUP_ENABLED=true`. It reads the administrator details from environment variables, uses the application's BCrypt password encoder, and does not print credentials. Use a password of 12 to 72 UTF-8 bytes. The command only creates the account if its email is unused and no administrator exists. Repeating it for that same administrator makes no changes; it will refuse to promote an existing citizen or create another admin. It never resets accounts or changes schema/data outside the new account row.

In PowerShell, first make sure `backend/.env` contains the local PostgreSQL settings described above, then run this from the `backend` directory. The admin password prompt is masked and the password is never placed in the Maven command line. The `finally` block removes the setup values even if Maven fails:

```powershell
$env:PUBLICCARE_ADMIN_SETUP_ENABLED = 'true'
try {
    $env:PUBLICCARE_ADMIN_FULL_NAME = Read-Host 'Administrator full name'
    $env:PUBLICCARE_ADMIN_EMAIL = Read-Host 'Administrator email'
    $securePassword = Read-Host 'Administrator password' -AsSecureString
    $env:PUBLICCARE_ADMIN_PASSWORD = [System.Net.NetworkCredential]::new('', $securePassword).Password
    ./mvnw.cmd "-Dspring-boot.run.main-class=lk.publiccare.backend.admin.AdminSetupApplication" spring-boot:run
}
finally {
    Remove-Item Env:PUBLICCARE_ADMIN_PASSWORD, Env:PUBLICCARE_ADMIN_FULL_NAME, Env:PUBLICCARE_ADMIN_EMAIL, Env:PUBLICCARE_ADMIN_SETUP_ENABLED -ErrorAction SilentlyContinue
    Remove-Variable securePassword -ErrorAction SilentlyContinue
}
```

The command requires the existing `users` table from V1. Afterwards, start the regular backend and sign in through the normal login endpoint using the entered email and password. Keep database and admin values in process environment variables or a local ignored `.env`; never add them to Git, source files, command arguments, or logs. The setup command is restricted to loopback PostgreSQL even if invoked with the explicit opt-in flag.

## Authentication API

The browser must include credentials (`credentials: "include"`) for API calls. CSRF protection is enabled for state-changing requests. First call `GET /api/auth/csrf`, retain the `XSRF-TOKEN` cookie, and send its value as the `X-XSRF-TOKEN` header on `POST /api/auth/register`, `POST /api/auth/login`, and `POST /api/auth/logout`. The session identifier is an HTTP-only cookie; do not put passwords or session credentials in localStorage.

### Get a CSRF token

```http
GET /api/auth/csrf
```

Returns `{ "token": "..." }` and sets the matching `XSRF-TOKEN` cookie.

### Register

```http
POST /api/auth/register
Content-Type: application/json
X-XSRF-TOKEN: <csrf-token>

{"fullName":"A Citizen","email":"citizen@example.com","password":"a-long-unique-password"}
```

Registration requires a full name (up to 120 characters), a valid email, and a password of 12–72 characters and no more than 72 UTF-8 bytes. Emails are trimmed and normalized to lowercase. New accounts always receive `CITIZEN`; a submitted role is ignored and cannot grant administrator access. Success returns `201` with the public account fields, never a password or password hash. Invalid input returns `400`; duplicate email returns `409`.

### Log in

```http
POST /api/auth/login
Content-Type: application/json
X-XSRF-TOKEN: <csrf-token>

{"email":"citizen@example.com","password":"a-long-unique-password"}
```

Success returns the public account fields and starts a server-side session. Invalid credentials return the same generic `401` response regardless of whether the email exists.

### Current account and logout

```http
GET /api/auth/me
POST /api/auth/logout
X-XSRF-TOKEN: <csrf-token>
```

`/api/auth/me` requires a valid session. Logout invalidates the session and clears its cookies. `/api/health` and `/actuator/health` remain public. Other API routes require authentication; `/api/admin/**` additionally requires the server-side `ADMIN` role.

## Reports API

All write calls require a valid `X-XSRF-TOKEN` header and the session cookie. The authenticated principal contains the database user ID. Report request bodies have no owner field; the backend always sets `owner_id` from that principal. Citizen report lookups include both the report ID and owner ID in the database criteria and return the same `404 Report not found` response for missing and other users' reports.

Report statuses are `PENDING`, `IN_PROGRESS`, and `RESOLVED`. PostgreSQL generates a reference as `PC-` plus the zero-padded report ID. Required values have these maximum lengths: title 160, description 5000, category 80, and location 240. A report may have one optional JPG, PNG, or WebP image up to 5 MiB. Image bytes are checked against their format signatures; JPG and PNG files are also decoded before acceptance. The request filename and declared content type are not used to determine the stored type.

### Create a report

```http
POST /api/reports
Content-Type: application/json
X-XSRF-TOKEN: <csrf-token>

{"title":"Large pothole near school","description":"A deep pothole beside the school entrance.","category":"Road damage","location":"Galle, near the school entrance"}
```

Returns `201` with the report including `id`, `reference`, `status`, owner display name, and timestamps. New reports start as `PENDING`.

### Upload and retrieve a report image

Create the report first, then upload its optional image. Upload requests require the same session and CSRF token as other writes:

```http
POST /api/reports/{id}/image
Content-Type: multipart/form-data
X-XSRF-TOKEN: <csrf-token>

image=<one JPG, PNG, or WebP file, maximum 5 MiB>
```

Success returns `204`. A second image for the report returns `409`; invalid or oversized files return `400` or `413`. The owner can retrieve it with `GET /api/reports/{id}/image`. Administrators can retrieve it with `GET /api/admin/reports/{id}/image`; the existing `/api/admin/**` role rule protects this route. Both image routes return private, non-cacheable bytes and return `404` for missing reports or images. Citizen access is scoped to the authenticated report owner.

### List the signed-in citizen's reports

```http
GET /api/reports/mine?page=0&size=10&sort=createdAt&direction=desc&status=PENDING&category=Road%20damage&q=school
```

Filters are optional. `status`, `category`, and `q` are applied to both `content` and `summary`; `q` searches title, reference, and location. Allowed sort fields are `createdAt`, `updatedAt`, `title`, `status`, and `category`; page is zero-based and size is 1–100. The response contains `content`, `page`, `size`, `totalElements`, `totalPages`, and matching status/category summary counts.

### View and edit a citizen report

`GET /api/reports/{id}` returns report details. `PUT /api/reports/{id}` accepts the same title, description, category, and location fields as create. Both operations are restricted to the report owner. Citizen edits cannot change status or ownership.

### Admin report management

`GET /api/admin/reports` accepts the same filters, paging, and sorting parameters as the citizen list. `GET /api/admin/reports/summary` returns global summary counts, with the same optional status/category/search filters. `GET /api/admin/reports/{id}` returns report details. `PATCH /api/admin/reports/{id}/status` changes status and an optional citizen-visible administrator note:

```json
{"status":"IN_PROGRESS","adminUpdateNote":"The local team has been notified."}
```

All `/api/admin/**` calls are protected by Spring Security's `ROLE_ADMIN` rule, regardless of the frontend route gate. Invalid input returns `400`; missing or non-owned citizen reports return `404`; unauthenticated calls return `401`; authenticated non-admin calls return `403`.

### Progress update history

`POST /api/admin/reports/{id}/progress-updates` accepts multipart form fields `status` (required; `PENDING`, `IN_PROGRESS`, or `RESOLVED`), `note` (optional, maximum 1000 characters), and `image` (optional JPG/PNG up to 5 MiB). Each successful request creates a distinct history record and updates current report status and latest note in the same operation. `GET /api/admin/reports/{id}/progress-updates` lists updates oldest first. Citizens use `GET /api/reports/{id}/progress-updates`; the backend checks report ownership. Progress photos are read through `GET /api/admin/reports/{id}/progress-updates/{updateId}/image` or the corresponding owner-scoped `/api/reports/{id}/progress-updates/{updateId}/image` route. Storage keys and filesystem paths are never returned.

## Build and tests

```powershell
./mvnw.cmd test
./mvnw.cmd package
```

From the project root, run `npm run lint` and `npm run build` to verify the React + Vite UI. For an end-to-end local run, configure PostgreSQL, start the backend so Flyway can apply migrations, then run `npm run dev` and use citizen registration/login before submitting reports.
