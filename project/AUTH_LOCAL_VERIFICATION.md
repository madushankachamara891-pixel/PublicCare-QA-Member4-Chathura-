# PublicCare authentication: local verification

These steps exercise the session and CSRF flow against the local app without creating an administrator or editing database credentials.

## Start the services

1. Start PostgreSQL with the existing `publiccare_db` database and the `users` table described by `backend/src/main/resources/db/migration/V1__create_users.sql`.
2. In `publiccare/backend`, start Spring Boot using the existing local environment configuration:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

   If the project has no Maven wrapper, use `mvn spring-boot:run` from the same directory.
3. In `publiccare`, start Vite with `npm run dev`. The browser app defaults to `http://localhost:8080` for the backend. Set `VITE_API_BASE_URL` only when the backend is at a different origin.

## Registration

1. Open `http://localhost:5173/register` and create a test Citizen account using a unique email, a name of at most 120 characters, and a password of 12–72 characters. Accept the terms checkbox.
2. In browser Developer Tools → Network, confirm the browser requests `GET /api/auth/csrf`, then sends `POST /api/auth/register` with `X-XSRF-TOKEN`, JSON content type, and credentials enabled. Expect `201 Created` and a response containing the account id, full name, email, role `CITIZEN`, and creation time. It must not contain a password or password hash.
3. Submit the same email again and expect `409 Conflict` with a user-facing duplicate-account message.

## Login, session, and logout

1. Sign in at `/login` with the test account. A wrong password should produce `401 Unauthorized` and the generic “Invalid email or password” message.
2. A correct password should produce `200 OK`, then navigate to `/my-reports`. The login response should set an HTTP-only `JSESSIONID` cookie. Refresh the page and confirm `GET /api/auth/me` returns the same Citizen account while the session is active.
3. Use the account menu’s Log out action. Expect `POST /api/auth/logout` with the CSRF header, then confirm a subsequent `GET /api/auth/me` returns `401 Unauthorized` and the account menu returns to its signed-out state.

## CSRF and role checks

1. While signed in, use the browser console on `http://localhost:5173` to send a state-changing request without a token:

   ```js
   fetch('http://localhost:8080/api/auth/logout', { method: 'POST', credentials: 'include' })
   ```

   Expect `403 Forbidden`. The same request from the app, which first obtains `/api/auth/csrf` and sends the returned token in `X-XSRF-TOKEN`, should return `204 No Content`.
2. Without a session, `GET /api/admin/issues` should return `401`. With a Citizen session, it should return `403`. The authorization rule permits only `ROLE_ADMIN`; currently there is no admin issue controller, so a valid ADMIN session reaches `404` at that path.
3. No default administrator account exists. To verify an actual ADMIN session, use an administrator account provisioned through the project’s approved operational process. Public registration always assigns `CITIZEN`.

## Environment before release

- Serve the frontend and backend over HTTPS and set `SESSION_COOKIE_SECURE=true` for the backend.
- Set `PUBLICCARE_CORS_ALLOWED_ORIGIN` to the exact deployed frontend origin. The default remains `http://localhost:5173` for local development.
- Set `VITE_API_BASE_URL` to the deployed backend origin at frontend build time.
- This integration authenticates accounts and gates the admin pages in the client. Issue submission, report ownership, and admin issue operations remain browser-local/demo workflows until their backend APIs and authorization checks are implemented.
