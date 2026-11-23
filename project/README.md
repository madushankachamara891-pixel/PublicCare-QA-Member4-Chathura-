# PublicCare

PublicCare is a React + Vite citizen issue reporting interface with a Spring Boot and PostgreSQL backend. Citizen reports are linked to the authenticated account; administrators can review reports and change their status.

## Local setup

1. Create the PostgreSQL database `publiccare_db`.
2. Start the backend; Flyway applies migrations V1 through V5 in order (including report images, progress history, and public approvals).
3. Configure `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` in the backend environment. See [backend setup and API verification](backend/README.md) for details.
4. Start the backend from `backend` with `./mvnw.cmd spring-boot:run`.
5. From this directory, run `npm install` once and `npm run dev`; open the Vite origin at `http://localhost:5173`.
6. Register or sign in before creating a report. The sample home page and fixture helpers remain demo content; signed-in report, detail, edit, and admin pages use the API.

The application uses `ddl-auto: none` and never creates an administrator during regular startup. For the first local development admin, follow the masked-password setup command in [backend setup and API verification](backend/README.md#create-the-first-local-administrator). Citizen-submitted report photos and administrator progress photos are stored privately outside the web root.

## Local verification

```powershell
cd backend
./mvnw.cmd test
cd ..
npm run lint
npm run build
```
