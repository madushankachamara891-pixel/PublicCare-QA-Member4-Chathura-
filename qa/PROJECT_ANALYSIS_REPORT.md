# PublicCare – QA Project Analysis Report (archived copy)

> **Test execution status: NOT EXECUTED.**
> All test cases proposed in this report (manual, automated UI, automated API and unit) are **proposals only**. None of them have been run, and no results, pass/fail data or defects are recorded here. The report was produced by a read-only review of the source code. Nothing was built, run or installed.

## About this document

| Item | Detail |
|---|---|
| Purpose | QA documentation for EC4206 – Software Testing and Quality Assurance, Part 01 (Requirement Analysis) |
| Content origin | The complete PublicCare QA Analysis Report produced on 23 Nov 2026, saved here as written |
| Code baseline analysed | git `f565fb4` ("Finalize report deletion and cleanup") in `publiccare/` |
| Location | `qa/PROJECT_ANALYSIS_REPORT.md` |

**Changes made when saving (formatting only; no finding, ID, count or wording of the report was changed):**
1. Two chat-only lines at the top of the original message were left out: an audience line, and a note that creating a shared document had been blocked. They described how the report was delivered, not its content.
2. Code links were re-pointed from `publiccare/...` to `../publiccare/...` so they still resolve from inside the `qa/` folder. The link text is unchanged.
3. This header and the closing "Editor's notes" appendix were added. Everything between them is the original report.

## How requirement certainty is marked

The report labels every requirement and business rule with an evidence code. These distinctions are kept exactly as in the original:

| Code | Meaning | Category |
|---|---|---|
| **C** | Confirmed in source code | Confirmed requirement |
| **C+D** | Confirmed in code and also documented | Confirmed requirement |
| **D** | Documented only, not confirmed in code | Documented-only (no item in this report carries this code) |
| **I** | Inferred from code. Not an explicit requirement; needs clarification or verification by test | Inferred requirement |
| **C / I** | The behaviour is confirmed in code, but whether it is *intended* needs clarification | Confirmed behaviour, intent requires clarification |

**Summary by category (taken from the report's own labels):**

- **Confirmed requirements:** all functional requirements FR-01 to FR-09 are tagged C or C+D. FR-08.5 is confirmed by the absence of a citizen delete route. Of the 28 business rules, 25 are tagged C or C+D.
- **Inferred requirements [I]:**
  - BR-22: an approval request body without `approved` defaults to `false` and revokes approval.
  - §2.1: no login rate limiting or account lockout was found.
  - §2.1: registration reveals whether an email exists (`409`).
  - §2.2: N+1 queries on the public feed.
  - §2.3: an orphan-file window when a failure happens at commit time.
  - §2.3: a concurrent duplicate image upload probably returns `500` instead of `409`.
  - §2.4: the edit page category falls back to the first option.
  - §2.4: the My Reports loading message does not reappear when filters change.
  - §2.6: the IPv6 loopback check (`[::1]`) may never match.
- **Confirmed behaviour whose intent needs clarification [C / I]:**
  - BR-09: citizens can edit reports at any status, including RESOLVED.
  - BR-13: there are no status transition rules.
- **Items the report says must be clarified before expected results are written** (listed in §7): BR-09, BR-13, BR-22, the concurrent duplicate upload (§2.3), the IPv6 loopback check (§2.6), the edit-page category fallback (§2.4) and the missing login throttling (§2.1). §2.2 also notes that no response-time targets are documented and that the group must define them.

## Where to find each part of the report

| Topic | Location in the report |
|---|---|
| Functional requirements FR-01 to FR-09 (with frontend, backend and code evidence) | TASK 1 |
| Non-functional requirements (security, performance, reliability, usability and accessibility, maintainability, compatibility, functional correctness) | TASK 2, §2.1–§2.7 |
| User roles and permissions matrix | TASK 3 |
| Business rules BR-01 to BR-28 | TASK 4 |
| Requirements Traceability Matrix (RTM) | TASK 5 |
| Proposed test cases: Manual (MT), Automated UI (UI), Automated API (API), Unit (UT) | TASK 6, sections A–D |
| Existing implementation evidence | "Evidence" columns and lists in TASK 1, TASK 2 and TASK 4; "Existing coverage" column in TASK 6-D |
| Risks | "Still needs testing / risk areas" (§2.1), "Needs testing" columns (§2.2–§2.7), "Risks" (§2.5) |
| Assumptions | The report has no separate assumptions section. Its working basis is stated under "Method" and "0. Scope and conventions", and its unconfirmed assumptions are the **[I]** items above |
| Recommendations | The report has no separate recommendations section. Its recommendations are inline: follow the code and update the docs (§7); clarify the inferred items before writing expected results (§7); define response-time targets (§2.2); verify the `Secure` cookie setting for deployment (§2.1); integration-test PostgreSQL-specific behaviour (§2.6); suggested test tools and adding Vitest for frontend unit tests (TASK 6) |
| Documentation gaps (docs that contradict the code) | §7 "Observations and documentation gaps", items 1–5 |

---

# EC4206 – Part 01: Requirement Analysis Report
**Project:** PublicCare, a citizen issue reporting and tracking platform
**Date of analysis:** 23 Nov 2026 · **Code baseline:** git `f565fb4` ("Finalize report deletion and cleanup")
**Method:** Read-only review of the source code, the DB migrations, `README.md`, `backend/README.md`, `AUTH_LOCAL_VERIFICATION.md` and the existing test classes. Nothing was run, built or installed. **None of the tests proposed here have been executed.**

### 0. Scope and conventions

| Item | Detail |
|---|---|
| Frontend | React 19 + Vite 8 SPA, `publiccare/src/`. Routing is done by hand from `window.location.pathname` ([App.jsx:55-64](../publiccare/src/App.jsx#L55-L64)) |
| Backend | Spring Boot 3.5.16, Java 21, Spring Security, JPA, Flyway, PostgreSQL (`publiccare/backend/`) |
| Database | Migrations V1–V5 ([db/migration](../publiccare/backend/src/main/resources/db/migration/)) |
| Existing tests | 73 backend `@Test` methods in 12 classes (JUnit 5, Mockito, `@WebMvcTest`, `@DataJpaTest` with H2). **No frontend test runner is configured**: [package.json:6-11](../publiccare/package.json#L6-L11) has only `dev`, `build`, `lint` and `preview` |

**Evidence legend**
- **C**: confirmed in source code.
- **C+D**: confirmed in code and also documented.
- **D**: documentation only, not confirmed in code.
- **I**: inferred from code. It is not an explicit requirement and needs clarification or verification by test.

Path shorthand used below: **B/** = `publiccare/backend/src/main/java/lk/publiccare/backend/`, **F/** = `publiccare/src/`, **M/** = `publiccare/backend/src/main/resources/db/migration/`.

---

## TASK 1: Functional Requirements

The suggested categories were kept, with these adjustments to match the code. FR-04 covers the citizen's management of their own reports. FR-06 includes first-admin provisioning. FR-08 is admin-only, because no citizen deletion exists.

### FR-01 User Registration

| ID | Requirement | Role | Preconditions | Expected behaviour | Priority | Testability | Src |
|---|---|---|---|---|---|---|---|
| FR-01.1 | Visitor can register a Citizen account with full name, email and password | Anonymous | Has CSRF token; email unused | `201 Created`, returns `id, fullName, email, role, createdAt`. Never returns a password or hash | High | High (API + UI) | C+D |
| FR-01.2 | Registration input is validated | Anonymous | – | `fullName` required, ≤120 chars. `email` must be a valid format, ≤254. `password` 12–72 chars and ≤72 UTF-8 bytes. Invalid input → `400` with the first field error | High | High (boundaries) | C+D |
| FR-01.3 | Email is trimmed and lower-cased; name is trimmed | – | – | `" A@X.com "` is stored as `a@x.com` | Medium | High | C+D |
| FR-01.4 | Duplicate email is rejected, including concurrent duplicates | Anonymous | Email exists | `409` "An account with this email already exists." | High | High (race case: Medium) | C+D |
| FR-01.5 | Public registration always assigns `CITIZEN` | Anonymous | – | Any submitted `role` is ignored (the DTO has no role field) | High | High | C+D |
| FR-01.6 | Client-side checks: password confirmation must match, Terms checkbox required, success message, form reset | Anonymous | – | Browser validation blocks submit. Terms acceptance is **not** sent to or enforced by the backend | Medium | High (UI) | C |
| FR-01.7 | Passwords are stored as BCrypt hashes (cost 12) | – | – | `password_hash` holds a BCrypt hash, never plaintext | High | Medium (DB inspection / unit) | C |

| ID | Frontend | Backend API / service | Evidence |
|---|---|---|---|
| FR-01.1 | `RegisterPage` | `POST /api/auth/register` → `AuthService.register` | [AuthController.java:43-46](../publiccare/backend/src/main/java/lk/publiccare/backend/auth/AuthController.java#L43-L46), [AuthService.java:23-37](../publiccare/backend/src/main/java/lk/publiccare/backend/auth/AuthService.java#L23-L37), [AccountPage.jsx:62-121](../publiccare/src/AccountPage.jsx#L62-L121) |
| FR-01.2 | `maxLength`/`minLength` attributes | `@Valid RegisterRequest` + byte check | [AuthDtos.java:13-16](../publiccare/backend/src/main/java/lk/publiccare/backend/auth/AuthDtos.java#L13-L16), [AuthService.java:25-27](../publiccare/backend/src/main/java/lk/publiccare/backend/auth/AuthService.java#L25-L27), [ApiExceptionHandler.java:23-29](../publiccare/backend/src/main/java/lk/publiccare/backend/exception/ApiExceptionHandler.java#L23-L29), [AccountPage.jsx:105-107](../publiccare/src/AccountPage.jsx#L105-L107) |
| FR-01.3 | – | `AuthService.register` | [AuthService.java:24](../publiccare/backend/src/main/java/lk/publiccare/backend/auth/AuthService.java#L24), [AuthService.java:29](../publiccare/backend/src/main/java/lk/publiccare/backend/auth/AuthService.java#L29) |
| FR-01.4 | Feedback paragraph | `existsByEmail` + `DataIntegrityViolationException` catch | [AuthService.java:28-36](../publiccare/backend/src/main/java/lk/publiccare/backend/auth/AuthService.java#L28-L36), [ApiExceptionHandler.java:36-40](../publiccare/backend/src/main/java/lk/publiccare/backend/exception/ApiExceptionHandler.java#L36-L40), [V1:8](../publiccare/backend/src/main/resources/db/migration/V1__create_users.sql#L8) |
| FR-01.5 | Register page copy | `UserRole.CITIZEN` hard-coded | [AuthService.java:30](../publiccare/backend/src/main/java/lk/publiccare/backend/auth/AuthService.java#L30) |
| FR-01.6 | `RegisterPage.handleSubmit` | – | [AccountPage.jsx:72-73](../publiccare/src/AccountPage.jsx#L72-L73), [AccountPage.jsx:83-84](../publiccare/src/AccountPage.jsx#L83-L84), [AccountPage.jsx:110](../publiccare/src/AccountPage.jsx#L110) |
| FR-01.7 | – | `BCryptPasswordEncoder(12)` | [WebSecurityConfig.java:31-34](../publiccare/backend/src/main/java/lk/publiccare/backend/config/WebSecurityConfig.java#L31-L34) |

### FR-02 Authentication, Session and Access Control

| ID | Requirement | Role | Preconditions | Expected behaviour | Priority | Testability | Src |
|---|---|---|---|---|---|---|---|
| FR-02.1 | A CSRF token is issued, and every state-changing request must carry it | All | – | `GET /api/auth/csrf` returns `{token}` and sets the `XSRF-TOKEN` cookie. POST/PUT/PATCH/DELETE without `X-XSRF-TOKEN` → `403` | High | High | C+D |
| FR-02.2 | Login with email and password starts a server-side session | Citizen / Admin | Account exists | `200` with user fields. HTTP-only `JSESSIONID`, SameSite=Lax. Session ID rotated (anti-fixation) | High | High | C+D |
| FR-02.3 | Invalid credentials give a generic error | Any | – | `401` "Invalid email or password." for both a wrong password and an unknown email | High | High | C+D |
| FR-02.4 | Login password over 72 UTF-8 bytes is rejected | Any | – | `400` (not `401`) | Low | High | C |
| FR-02.5 | Current-user lookup | Authenticated | Session | `GET /api/auth/me` → `200` user. Without a session → `401 {"error":"Authentication required."}` | High | High | C+D |
| FR-02.6 | Logout | Authenticated | Session + CSRF | `POST /api/auth/logout` → `204`. Session invalidated, `JSESSIONID` and `XSRF-TOKEN` deleted. UI returns to `/` | High | High | C+D |
| FR-02.7 | Role-based landing after login | Citizen / Admin | – | ADMIN → `/admin`, CITIZEN → `/my-reports` | Medium | High (UI) | C |
| FR-02.8 | Server-side authorisation | All | – | Public: health, `GET /api/public/reports[/*]`, register/login/csrf. `/api/admin/**` needs `ROLE_ADMIN` (`403` otherwise). Everything else needs authentication (`401`) | High | High | C+D |
| FR-02.9 | Client-side admin page gate | Non-admin | – | `/admin`, `/admin/issues`, `/admin/update-issue` show "Administrator access required" for non-admins | Medium | High (UI) | C |

| ID | Frontend | Backend | Evidence |
|---|---|---|---|
| FR-02.1 | `authApi.csrfToken`, `reportsApi.request(…, csrf=true)` | `CookieCsrfTokenRepository` | [WebSecurityConfig.java:24-29](../publiccare/backend/src/main/java/lk/publiccare/backend/config/WebSecurityConfig.java#L24-L29), [WebSecurityConfig.java:55-56](../publiccare/backend/src/main/java/lk/publiccare/backend/config/WebSecurityConfig.java#L55-L56), [AuthController.java:74-77](../publiccare/backend/src/main/java/lk/publiccare/backend/auth/AuthController.java#L74-L77), [authApi.js:29-57](../publiccare/src/authApi.js#L29-L57) |
| FR-02.2 | `LoginPage` | `POST /api/auth/login` | [AuthController.java:48-67](../publiccare/backend/src/main/java/lk/publiccare/backend/auth/AuthController.java#L48-L67), [WebSecurityConfig.java:46-49](../publiccare/backend/src/main/java/lk/publiccare/backend/config/WebSecurityConfig.java#L46-L49), [application.yml:41-46](../publiccare/backend/src/main/resources/application.yml#L41-L46) |
| FR-02.3 | `login-feedback` alert | `InvalidCredentialsException` | [AuthController.java:55-60](../publiccare/backend/src/main/java/lk/publiccare/backend/auth/AuthController.java#L55-L60), [ApiExceptionHandler.java:42-46](../publiccare/backend/src/main/java/lk/publiccare/backend/exception/ApiExceptionHandler.java#L42-L46) |
| FR-02.4 | – | byte check | [AuthController.java:51-53](../publiccare/backend/src/main/java/lk/publiccare/backend/auth/AuthController.java#L51-L53) |
| FR-02.5 | `AuthProvider.refresh` | `GET /api/auth/me` | [AuthController.java:69-72](../publiccare/backend/src/main/java/lk/publiccare/backend/auth/AuthController.java#L69-L72), [WebSecurityConfig.java:65-70](../publiccare/backend/src/main/java/lk/publiccare/backend/config/WebSecurityConfig.java#L65-L70), [AuthContext.jsx:10-27](../publiccare/src/AuthContext.jsx#L10-L27) |
| FR-02.6 | `SiteHeader.handleLogout` | Spring logout config | [WebSecurityConfig.java:78-82](../publiccare/backend/src/main/java/lk/publiccare/backend/config/WebSecurityConfig.java#L78-L82), [SiteHeader.jsx:53-66](../publiccare/src/SiteHeader.jsx#L53-L66) |
| FR-02.7 | `LoginPage.handleSubmit` | – | [AccountPage.jsx:32](../publiccare/src/AccountPage.jsx#L32) |
| FR-02.8 | – | `authorizeHttpRequests` | [WebSecurityConfig.java:59-75](../publiccare/backend/src/main/java/lk/publiccare/backend/config/WebSecurityConfig.java#L59-L75) |
| FR-02.9 | `AdminAccessGate` | – | [App.jsx:17-22](../publiccare/src/App.jsx#L17-L22), [App.jsx:57-59](../publiccare/src/App.jsx#L57-L59) |

### FR-03 Issue Reporting

| ID | Requirement | Role | Preconditions | Expected behaviour | Priority | Testability | Src |
|---|---|---|---|---|---|---|---|
| FR-03.1 | Authenticated user submits a report (title, category, description, location) | Citizen (and Admin, see Task 3) | Logged in + CSRF | `201`. Status `PENDING`. Reference `PC-` + 8-digit zero-padded ID. Owner name and timestamps returned | High | High | C+D |
| FR-03.2 | Report fields are validated and trimmed | Citizen | – | All required and non-blank. Max lengths: title 160, description 5000, category 80, location 240. Otherwise `400` | High | High (boundaries) | C+D |
| FR-03.3 | Owner comes from the session, never from the request | Citizen | – | An `ownerId` in the body is ignored | High | High | C+D |
| FR-03.4 | UI offers 6 fixed categories | Citizen | – | Road damage, Waste & garbage, Streetlights, Drainage, Water supply, Other public issue. **The backend accepts any string ≤80** | Medium | High | C |
| FR-03.5 | One optional image per report, uploaded after creation, owner only | Citizen (owner) | Report exists, no image yet | `POST /api/reports/{id}/image` (multipart `image`) → `204`. Second upload → `409`. Non-owner → `404` | High | High | C+D |
| FR-03.6 | Server-side image validation | – | – | 1 byte to 5 MiB. Type is detected from magic bytes (JPG/PNG/WebP), not from filename or declared type. JPG/PNG are fully decoded. ≤10 megapixels. Invalid → `400`. Multipart over 5 MB → `413` | High | High | C+D |
| FR-03.7 | Client-side image checks and recovery | Citizen | – | JPG/PNG only (extension + MIME), not empty, ≤5 MiB. Preview, remove, and "Retry image upload" if the upload fails after the report was created | Medium | High (UI) | C |
| FR-03.8 | Signed-out user cannot submit | Anonymous | – | Submit button disabled and a "Please sign in" notice is shown (the page itself is not gated) | Medium | High (UI) | C |

| ID | Frontend | Backend | Evidence |
|---|---|---|---|
| FR-03.1 | `CreateIssue.handleSubmit` | `POST /api/reports` → `ReportService.create` | [ReportController.java:35-39](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportController.java#L35-L39), [ReportService.java:28-35](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportService.java#L28-L35), [V2:3](../publiccare/backend/src/main/resources/db/migration/V2__create_reports.sql#L3), [V2:9-10](../publiccare/backend/src/main/resources/db/migration/V2__create_reports.sql#L9-L10), [CreateIssue.jsx:79-95](../publiccare/src/CreateIssue.jsx#L79-L95) |
| FR-03.2 | `required`/`maxLength` | `CreateRequest` constraints, `clean()` | [ReportDtos.java:11-14](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportDtos.java#L11-L14), [ReportService.java:157](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportService.java#L157), [CreateIssue.jsx:107-117](../publiccare/src/CreateIssue.jsx#L107-L117) |
| FR-03.3 | – | principal → `getUserId()` | [ReportController.java:38](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportController.java#L38), [PublicCarePrincipal.java:8-16](../publiccare/backend/src/main/java/lk/publiccare/backend/security/PublicCarePrincipal.java#L8-L16) |
| FR-03.4 | `<select id="issue-category">` | – | [CreateIssue.jsx:110-112](../publiccare/src/CreateIssue.jsx#L110-L112) |
| FR-03.5 | `uploadSelectedImage` | `ReportImageService.upload` | [ReportController.java:41-45](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportController.java#L41-L45), [ReportImageService.java:28-49](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportImageService.java#L28-L49), [V3:8](../publiccare/backend/src/main/resources/db/migration/V3__create_report_images.sql#L8) |
| FR-03.6 | – | `validateImage`, `isWebp`, multipart limits | [ReportImageService.java:15-16](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportImageService.java#L15-L16), [ReportImageService.java:68-135](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportImageService.java#L68-L135), [application.yml:20-23](../publiccare/backend/src/main/resources/application.yml#L20-L23), [ApiExceptionHandler.java:64-67](../publiccare/backend/src/main/java/lk/publiccare/backend/exception/ApiExceptionHandler.java#L64-L67) |
| FR-03.7 | `handleImageChange`, retry button | – | [CreateIssue.jsx:7-8](../publiccare/src/CreateIssue.jsx#L7-L8), [CreateIssue.jsx:27-77](../publiccare/src/CreateIssue.jsx#L27-L77), [CreateIssue.jsx:133-135](../publiccare/src/CreateIssue.jsx#L133-L135) |
| FR-03.8 | notice + `disabled` | – | [CreateIssue.jsx:130](../publiccare/src/CreateIssue.jsx#L130), [CreateIssue.jsx:136](../publiccare/src/CreateIssue.jsx#L136) |

### FR-04 Issue Management (citizen's own reports)

| ID | Requirement | Role | Preconditions | Expected behaviour | Priority | Testability | Src |
|---|---|---|---|---|---|---|---|
| FR-04.1 | List own reports with paging, sort, filters and summary | Citizen | Logged in | `GET /api/reports/mine`. Page ≥0, size 1–100, sort ∈ {createdAt, updatedAt, title, status, category}, direction asc/desc. Filters: `status`, `category`, `q` (case-insensitive on title, reference, location). Summary counts use the same filters. UI uses size 10 with status and search only (**no category filter in the UI**) | High | High | C+D |
| FR-04.2 | View own report detail | Citizen (owner) | – | `GET /api/reports/{id}` → `200`. Another user's report and a missing report both give the **same** `404 "Report not found."` | High | High | C+D |
| FR-04.3 | View own report photo | Citizen (owner) | Image exists | `GET /api/reports/{id}/image` → bytes with `Cache-Control: no-store` and `X-Content-Type-Options: nosniff`. No image → `404` (UI: "No photo was attached") | Medium | High | C+D |
| FR-04.4 | Edit own report | Citizen (owner) | – | `PUT /api/reports/{id}` with the same fields and rules as create. Cannot change status or owner. Allowed at **any** status | High | High | C+D |
| FR-04.5 | Editing removes public approval | Citizen | Report approved | After a successful PUT, `publicApproved` = false | High | High | C |
| FR-04.6 | Photo cannot be changed on edit | Citizen | – | UI says "Photo attachments are not supported yet". There is no replace/delete image endpoint | Low | High | C |
| FR-04.7 | Invalid list parameters are rejected | Citizen | – | Unknown status → `400` "Unsupported report status.". Unknown sort → `400`. `ALL` or blank status means no filter. `"in progress"` is normalised to `IN_PROGRESS` | Medium | High | C |

| ID | Frontend | Backend | Evidence |
|---|---|---|---|
| FR-04.1 | `MyReports` | `ReportService.mine/list/findPage/summarize/criteria` | [ReportController.java:60-67](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportController.java#L60-L67), [ReportService.java:37-74](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportService.java#L37-L74), [ReportService.java:119-155](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportService.java#L119-L155), [MyReports.jsx:20-60](../publiccare/src/MyReports.jsx#L20-L60) |
| FR-04.2 | `IssueDetails` | `mineDetail` + `ownerAndId` | [ReportService.java:81-84](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportService.java#L81-L84), [ReportService.java:133-135](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportService.java#L133-L135), [ApiExceptionHandler.java:48-51](../publiccare/backend/src/main/java/lk/publiccare/backend/exception/ApiExceptionHandler.java#L48-L51) |
| FR-04.3 | `IssueDetails` photo section | `citizenImage`, `imageResponse` | [ReportController.java:47-58](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportController.java#L47-L58), [ReportImageService.java:51-55](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportImageService.java#L51-L55), [IssueDetails.jsx:39-51](../publiccare/src/IssueDetails.jsx#L39-L51) |
| FR-04.4 | `EditIssue.saveChanges` | `updateMine` | [ReportController.java:74-78](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportController.java#L74-L78), [ReportService.java:91-98](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportService.java#L91-L98), [EditIssue.jsx:20-30](../publiccare/src/EditIssue.jsx#L20-L30) |
| FR-04.5 | – | `revokePublicApproval()` | [ReportService.java:96](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportService.java#L96) |
| FR-04.6 | hint text | (absence of endpoint) | [EditIssue.jsx:48](../publiccare/src/EditIssue.jsx#L48), [ReportController.java](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportController.java) |
| FR-04.7 | – | `findPage`, `criteria` | [ReportService.java:63-71](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportService.java#L63-L71), [ReportService.java:141-146](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportService.java#L141-L146) |

### FR-05 Issue Tracking

| ID | Requirement | Role | Preconditions | Expected behaviour | Priority | Testability | Src |
|---|---|---|---|---|---|---|---|
| FR-05.1 | Status timeline on the report page | Citizen (owner) | Report loaded | 4 steps: Issue submitted → Under review → In progress → Resolved, marked from the current status (PENDING = "Under review" current) | Medium | High (UI) | C |
| FR-05.2 | Latest administrator note shown to the owner | Citizen (owner) | Admin note set | "Latest Update from PublicCare" box. Otherwise a placeholder | Medium | High | C |
| FR-05.3 | Full progress history visible to the owner | Citizen (owner) | – | `GET /api/reports/{id}/progress-updates`, oldest first, with **all** notes (owners are not subject to public approval) and progress photos via the owner-scoped image route. Non-owner → `404` | High | High | C+D |
| FR-05.4 | Reference number shown for tracking | Citizen | – | Shown after submit, in My Reports, and on the detail banner | Medium | High | C |

Evidence:
- FR-05.1: [IssueDetails.jsx:88-94](../publiccare/src/IssueDetails.jsx#L88-L94), [reportsData.js:16-28](../publiccare/src/reportsData.js#L16-L28)
- FR-05.2: [IssueDetails.jsx:95-99](../publiccare/src/IssueDetails.jsx#L95-L99), [ReportDtos.java:21-24](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportDtos.java#L21-L24)
- FR-05.3: [ReportController.java:30-33](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportController.java#L30-L33), [ReportProgressService.java:41-55](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportProgressService.java#L41-L55), [IssueDetails.jsx:28-38](../publiccare/src/IssueDetails.jsx#L28-L38), [IssueDetails.jsx:100](../publiccare/src/IssueDetails.jsx#L100)
- FR-05.4: [CreateIssue.jsx:134](../publiccare/src/CreateIssue.jsx#L134), [MyReports.jsx:56](../publiccare/src/MyReports.jsx#L56), [IssueDetails.jsx:69](../publiccare/src/IssueDetails.jsx#L69)

### FR-06 Admin Management

| ID | Requirement | Role | Preconditions | Expected behaviour | Priority | Testability | Src |
|---|---|---|---|---|---|---|---|
| FR-06.1 | Admin dashboard | Admin | Admin session | Global counts (total, pending, in progress, resolved, per category) plus the 5 most recent reports | Medium | High | C |
| FR-06.2 | List and search all reports | Admin | – | `GET /api/admin/reports` with the same parameters as FR-04.1, across all owners. UI: search, category, status, date sort, 10 rows per page, Reset | High | High | C+D |
| FR-06.3 | Export the current page as CSV | Admin | Rows present | Client-side CSV `publiccare-reports.csv` of the **current page only**, with quotes escaped | Low | Medium | C |
| FR-06.4 | View any report in full | Admin | – | `GET /api/admin/reports/{id}` adds `publicApproved` and owner name/ID. Citizen photo via `/api/admin/reports/{id}/image` | High | High | C+D |
| FR-06.5 | Add a progress update (status, optional note, optional photo) atomically | Admin | CSRF | `POST …/progress-updates` (multipart). Status required. Note ≤1000. Photo JPG/PNG ≤5 MiB. Creates a history row **and** sets the report's current status and latest note. `201`. An invalid image leaves no partial update | High | High | C+D |
| FR-06.6 | Direct status/note change | Admin | CSRF | `PATCH /api/admin/reports/{id}/status` `{status, adminUpdateNote}`. Null status → `400`. Note ≤1000. A null note **clears** the note. **API only**: `updateReportStatus` exists in `reportsApi.js` but no page uses it | Medium | High (API) | C+D |
| FR-06.7 | View progress history with approval flags and photos | Admin | – | `GET …/progress-updates` (oldest first) and `…/{updateId}/image` | Medium | High | C+D |
| FR-06.8 | "Mark as resolved" helper | Admin | – | Sets the status dropdown to RESOLVED only. Nothing is saved until "Save Changes" | Low | High (UI) | C |
| FR-06.9 | First-admin provisioning (non-web CLI) | Operator | `PUBLICCARE_ADMIN_SETUP_ENABLED=true`, loopback PostgreSQL | Creates an ADMIN only if none exists and the email is unused. Re-running for the same admin makes no change. Refuses to promote a citizen or create a second admin. Password 12 chars to 72 bytes | Medium | High (unit) | C+D |

Evidence:
- FR-06.1: [AdminDashboard.jsx:32-39](../publiccare/src/AdminDashboard.jsx#L32-L39), [AdminReportController.java:49-53](../publiccare/backend/src/main/java/lk/publiccare/backend/report/AdminReportController.java#L49-L53)
- FR-06.2: [AdminReportController.java:41-47](../publiccare/backend/src/main/java/lk/publiccare/backend/report/AdminReportController.java#L41-L47), [ReportService.java:43-50](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportService.java#L43-L50), [AdminIssues.jsx:32-50](../publiccare/src/AdminIssues.jsx#L32-L50), [AdminIssues.jsx:92-98](../publiccare/src/AdminIssues.jsx#L92-L98)
- FR-06.3: [AdminIssues.jsx:10-16](../publiccare/src/AdminIssues.jsx#L10-L16)
- FR-06.4: [AdminReportController.java:55-56](../publiccare/backend/src/main/java/lk/publiccare/backend/report/AdminReportController.java#L55-L56), [AdminReportController.java:71-74](../publiccare/backend/src/main/java/lk/publiccare/backend/report/AdminReportController.java#L71-L74), [AdminReportDtos.java:9-12](../publiccare/backend/src/main/java/lk/publiccare/backend/report/AdminReportDtos.java#L9-L12)
- FR-06.5: [AdminReportController.java:33-39](../publiccare/backend/src/main/java/lk/publiccare/backend/report/AdminReportController.java#L33-L39), [ReportProgressService.java:18-39](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportProgressService.java#L18-L39), [AdminUpdateIssue.jsx:72-83](../publiccare/src/AdminUpdateIssue.jsx#L72-L83)
- FR-06.6: [AdminReportController.java:76-79](../publiccare/backend/src/main/java/lk/publiccare/backend/report/AdminReportController.java#L76-L79), [ReportService.java:100-108](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportService.java#L100-L108), [reportsApi.js:68](../publiccare/src/reportsApi.js#L68)
- FR-06.7: [AdminReportController.java:29-32](../publiccare/backend/src/main/java/lk/publiccare/backend/report/AdminReportController.java#L29-L32), [ReportProgressService.java:46-60](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportProgressService.java#L46-L60)
- FR-06.8: [AdminUpdateIssue.jsx:151](../publiccare/src/AdminUpdateIssue.jsx#L151)
- FR-06.9: [AdminSetupRunner.java:24-52](../publiccare/backend/src/main/java/lk/publiccare/backend/admin/AdminSetupRunner.java#L24-L52), [AdminAccountSetupService.java:25-60](../publiccare/backend/src/main/java/lk/publiccare/backend/admin/AdminAccountSetupService.java#L25-L60)

### FR-07 Public Report Visibility

| ID | Requirement | Role | Preconditions | Expected behaviour | Priority | Testability | Src |
|---|---|---|---|---|---|---|---|
| FR-07.1 | Approve or revoke a report for public display | Admin | CSRF | `PATCH …/{id}/public-approval {approved}` → `{approved}`. Records the approver ID and time | High | High | C |
| FR-07.2 | Approve or revoke an individual progress update | Admin | Update belongs to the report | `PATCH …/progress-updates/{updateId}/public-approval`. Wrong report/update pair → `404` | High | High | C |
| FR-07.3 | Approve or revoke a progress **note** separately | Admin | Note is non-blank | `PATCH …/note-public-approval`. Blank note → `400`. UI shows "No note to approve" | High | High | C |
| FR-07.4 | Anonymous public feed | Anonymous | – | `GET /api/public/reports` (page ≥0, size 1–24, default 9). Approved reports only, newest first. Each report carries up to 10 newest **approved** updates. A note appears only if the note is also approved | High | High | C |
| FR-07.5 | Anonymous public detail | Anonymous | – | `GET /api/public/reports/{id}`. Unapproved or missing → `404`. UI route `/public-reports/{digits}` | High | High | C |
| FR-07.6 | Public data minimisation | Anonymous | – | Only id, title, category, status, location, description, reportedAt and {status, createdAt, approved note}. **No** reference, owner, admin note or images | High | High | C |
| FR-07.7 | Automatic revocation of report approval | – | Report approved | Revoked on a citizen edit, a PATCH status *change*, or a progress update with a *different* status. Not revoked when the status is unchanged | High | High | C |

Evidence:
- FR-07.1: [AdminReportController.java:81-85](../publiccare/backend/src/main/java/lk/publiccare/backend/report/AdminReportController.java#L81-L85), [ReportService.java:110-117](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportService.java#L110-L117), [PublicCareReport.java:96-101](../publiccare/backend/src/main/java/lk/publiccare/backend/report/PublicCareReport.java#L96-L101), [V5:1-13](../publiccare/backend/src/main/resources/db/migration/V5__add_public_report_approvals.sql#L1-L13), [AdminIssues.jsx:51-59](../publiccare/src/AdminIssues.jsx#L51-L59)
- FR-07.2/.3: [AdminReportController.java:87-97](../publiccare/backend/src/main/java/lk/publiccare/backend/report/AdminReportController.java#L87-L97), [ReportProgressService.java:61-78](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportProgressService.java#L61-L78), [ReportProgressUpdate.java:32-38](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportProgressUpdate.java#L32-L38), [V5:40-44](../publiccare/backend/src/main/resources/db/migration/V5__add_public_report_approvals.sql#L40-L44)
- FR-07.4/.5: [PublicReportController.java:18-27](../publiccare/backend/src/main/java/lk/publiccare/backend/report/PublicReportController.java#L18-L27), [PublicReportService.java:12-56](../publiccare/backend/src/main/java/lk/publiccare/backend/report/PublicReportService.java#L12-L56), [ReportProgressUpdateRepository.java:8](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportProgressUpdateRepository.java#L8), [PublicReports.jsx](../publiccare/src/PublicReports.jsx), [PublicReportDetails.jsx](../publiccare/src/PublicReportDetails.jsx), [App.jsx:55-56](../publiccare/src/App.jsx#L55-L56)
- FR-07.6: [PublicReportDtos.java:9-12](../publiccare/backend/src/main/java/lk/publiccare/backend/report/PublicReportDtos.java#L9-L12)
- FR-07.7: [ReportService.java:96](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportService.java#L96), [ReportService.java:104](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportService.java#L104), [ReportProgressService.java:33](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportProgressService.java#L33)

### FR-08 Report Deletion

| ID | Requirement | Role | Preconditions | Expected behaviour | Priority | Testability | Src |
|---|---|---|---|---|---|---|---|
| FR-08.1 | Permanent deletion of any report | Admin | CSRF | `DELETE /api/admin/reports/{id}` → `204`. The report, its image row and all progress updates are removed in one transaction, whatever the status or approval | High | High | C |
| FR-08.2 | Stored files are deleted only after commit | – | – | Files are removed after commit. A file-removal error is logged and does not fail the request. If the DB delete fails, no files are touched | Medium | Medium (unit / JPA) | C |
| FR-08.3 | Error handling | Admin | – | Missing → `404`. Any other failure → `500 "Unable to delete report."` with the details logged on the server only | Medium | High | C |
| FR-08.4 | Delete UI safeguards | Admin | – | `window.confirm` naming the reference. All Delete buttons disabled while one is pending. List reloads afterwards | Medium | High (UI) | C |
| FR-08.5 | Citizens cannot delete reports | Citizen | – | No delete route exists under `/api/reports`. Admin route → `403` for citizens | High | High | C (confirmed by absence) |

Evidence: [ReportDeletionService.java:29-65](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportDeletionService.java#L29-L65), [AdminReportController.java:58-69](../publiccare/backend/src/main/java/lk/publiccare/backend/report/AdminReportController.java#L58-L69), [ApiExceptionHandler.java:53-57](../publiccare/backend/src/main/java/lk/publiccare/backend/exception/ApiExceptionHandler.java#L53-L57), [AdminIssues.jsx:61-70](../publiccare/src/AdminIssues.jsx#L61-L70), [ReportController.java](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportController.java) (no `@DeleteMapping`).

### FR-09 Platform Support (added category)

| ID | Requirement | Role | Expected behaviour | Priority | Testability | Evidence | Src |
|---|---|---|---|---|---|---|---|
| FR-09.1 | Public health checks | Anonymous | `GET /api/health` → `{"status":"UP"}`. `/actuator/health` includes DB health | Low | High | [HealthController.java](../publiccare/backend/src/main/java/lk/publiccare/backend/health/HealthController.java), [application.yml:29-36](../publiccare/backend/src/main/resources/application.yml#L29-L36) | C+D |
| FR-09.2 | Schema managed only by Flyway (V1–V5); Hibernate `ddl-auto: none` | – | Migrations apply in order. Existing DBs are baselined at V2 | Medium | Medium | [application.yml:13-19](../publiccare/backend/src/main/resources/application.yml#L13-L19) | C+D |

---

## TASK 2: Non-Functional Requirements

### 2.1 Security

| Implemented (confirmed) | Evidence |
|---|---|
| Session-based authentication. HTTP-only session cookie, SameSite=Lax, `Secure` configurable | [application.yml:41-46](../publiccare/backend/src/main/resources/application.yml#L41-L46) |
| Session ID rotated on login (session-fixation defence) | [WebSecurityConfig.java:46-49](../publiccare/backend/src/main/java/lk/publiccare/backend/config/WebSecurityConfig.java#L46-L49) |
| CSRF protection (cookie + header) on all writes | [WebSecurityConfig.java:55-56](../publiccare/backend/src/main/java/lk/publiccare/backend/config/WebSecurityConfig.java#L55-L56) |
| BCrypt (cost 12). 72-byte limit enforced, so BCrypt truncation cannot occur | [WebSecurityConfig.java:33](../publiccare/backend/src/main/java/lk/publiccare/backend/config/WebSecurityConfig.java#L33), [AuthService.java:25](../publiccare/backend/src/main/java/lk/publiccare/backend/auth/AuthService.java#L25) |
| Generic login error (no user enumeration through login) | [ApiExceptionHandler.java:42-46](../publiccare/backend/src/main/java/lk/publiccare/backend/exception/ApiExceptionHandler.java#L42-L46) |
| Role checks on the server, independent of the frontend gate | [WebSecurityConfig.java:63](../publiccare/backend/src/main/java/lk/publiccare/backend/config/WebSecurityConfig.java#L63), [AdminReportController.java:99-105](../publiccare/backend/src/main/java/lk/publiccare/backend/report/AdminReportController.java#L99-L105) |
| Owner-scoped queries. Same `404` for non-owned and missing reports (prevents ID probing) | [ReportService.java:133-135](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportService.java#L133-L135) |
| Image hardening: magic-byte detection, decode check, 10 MP cap, `nosniff`, `no-store`, private storage outside the web root, UUID keys, path-traversal guard | [ReportImageService.java:68-135](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportImageService.java#L68-L135), [PrivateImageStorage.java:47-54](../publiccare/backend/src/main/java/lk/publiccare/backend/report/PrivateImageStorage.java#L47-L54) |
| Public DTO minimisation. Approval state kept out of citizen DTOs | [PublicReportDtos.java](../publiccare/backend/src/main/java/lk/publiccare/backend/report/PublicReportDtos.java), [AdminReportDtos.java:5](../publiccare/backend/src/main/java/lk/publiccare/backend/report/AdminReportDtos.java#L5) |
| Error messages hidden (`include-message: never`). Passwords redacted in `toString` | [application.yml:39-40](../publiccare/backend/src/main/resources/application.yml#L39-L40), [AuthDtos.java:17-28](../publiccare/backend/src/main/java/lk/publiccare/backend/auth/AuthDtos.java#L17-L28) |
| CORS limited to one configured origin with a header allow-list | [WebConfig.java:11-22](../publiccare/backend/src/main/java/lk/publiccare/backend/config/WebConfig.java#L11-L22) |
| Secrets kept out of git: only `backend/.env.example` is tracked | `.gitignore`, `git ls-files` |
| No `dangerouslySetInnerHTML` in the frontend, so React escaping applies | grep of `src/` |

**Still needs testing / risk areas**
- **[I] No login rate limiting or account lockout** was found. Brute-force resistance is untested.
- Password policy is length-only (12–72). No complexity or breached-password check.
- The `Secure` cookie flag defaults to `false` ([WebSecurityConfig.java:25](../publiccare/backend/src/main/java/lk/publiccare/backend/config/WebSecurityConfig.java#L25)). Deployment configuration must be verified.
- The `XSRF-TOKEN` cookie is not HTTP-only by design (the SPA has to read it). XSS resistance therefore matters; test with script payloads in title, description and notes.
- **[I] Registration leaks whether an email exists** (`409`), even though login does not.
- WebP is checked by structure only, not decoded ([ReportImageService.java:77](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportImageService.java#L77)). Candidate for malformed-file fuzzing.
- Access-control (IDOR) testing is needed on every `/{id}` and `/{updateId}` route.

### 2.2 Performance

| Implemented | Needs testing |
|---|---|
| Pagination caps: 100 (citizen/admin), 24 (public) ([ReportService.java:63](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportService.java#L63), [PublicReportService.java:12](../publiccare/backend/src/main/java/lk/publiccare/backend/report/PublicReportService.java#L12)) | **[I] N+1 queries on the public feed**: 1 history query per report, so up to 25 queries per page ([PublicReportService.java:31-34](../publiccare/backend/src/main/java/lk/publiccare/backend/report/PublicReportService.java#L31-L34)) |
| Indexes on owner/created and status/created, plus partial public-feed indexes ([V2:18-19](../publiccare/backend/src/main/resources/db/migration/V2__create_reports.sql#L18-L19), [V5:46-52](../publiccare/backend/src/main/resources/db/migration/V5__add_public_report_approvals.sql#L46-L52)) | Each list call also runs 5 summary queries ([ReportService.java:119-131](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportService.java#L119-L131)) |
| Upload size limits stop oversized requests early | `LIKE '%q%'` search cannot use an index, so it slows as data grows |
| `open-in-view: false` | Images are read fully into memory (`byte[]`, up to 5 MiB each). The admin page fetches every progress photo in parallel ([AdminUpdateIssue.jsx:41-45](../publiccare/src/AdminUpdateIssue.jsx#L41-L45)) |
| | BCrypt-12 login cost under concurrent load. **No response-time targets are documented**, so the group must define them |

### 2.3 Reliability

| Implemented | Needs testing |
|---|---|
| Transactions on all writes. Compensating file delete if the DB save fails ([ReportImageService.java:42-48](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportImageService.java#L42-L48), [ReportProgressService.java:32-38](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportProgressService.java#L32-L38)) | **[I] Orphan-file window**: the file is written before the transaction commits, and the catch only covers `saveAndFlush`. A failure at commit time would leave a file behind |
| File cleanup after commit on delete ([ReportDeletionService.java:52-64](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportDeletionService.java#L52-L64)) | **[I] Concurrent duplicate image upload**: the unique constraint raises `DataIntegrityViolationException`, which has no handler, so the response is probably `500` instead of `409` |
| Concurrent duplicate-registration race handled ([AuthService.java:33-36](../publiccare/backend/src/main/java/lk/publiccare/backend/auth/AuthService.java#L33-L36)) | Behaviour when the upload directory is missing or unwritable (`IllegalStateException` → 500) |
| DB integrity: CHECK constraints on status, image type and size, and approval actor/time | Frontend behaviour when the backend is unreachable ("Unable to reach PublicCare…") |

### 2.4 Usability and accessibility

Implemented:
- `role="alert"`/`role="status"` messages and `aria-live` regions.
- Keyboard-operable account menu (arrow keys, Home/End, Escape returns focus) ([SiteHeader.jsx:32-94](../publiccare/src/SiteHeader.jsx#L32-L94)).
- Respects `prefers-reduced-motion` ([SiteHeader.jsx:19](../publiccare/src/SiteHeader.jsx#L19)).
- Show/hide password, image preview, retry upload, loading and empty states.

Needs testing:
- A full WCAG 2.1 AA audit: contrast, focus order, labels.
- Responsive layouts.
- Dates use the browser locale.
- **[I] Edit category issue**: if a stored category is not one of the 6 UI options (possible through the API), the edit `<select>` falls back to the first option and silently changes the category on save ([EditIssue.jsx:44](../publiccare/src/EditIssue.jsx#L44)).
- **[I] My Reports loading message**: it does not reappear when filters change, because `loading` is never set to true in the effect ([MyReports.jsx:28-35](../publiccare/src/MyReports.jsx#L28-L35)).
- Editing silently removes public visibility, and the citizen is not told.

### 2.5 Maintainability

Implemented:
- Packages are split by feature (auth, report, security, admin).
- Request/response objects are Java records.
- Flyway handles schema changes.
- ESLint is configured.
- There are 73 backend test methods.
- READMEs exist.

Risks:
- No frontend tests or test runner.
- Duplicated `request()` helpers in [authApi.js](../publiccare/src/authApi.js) and [reportsApi.js](../publiccare/src/reportsApi.js).
- Two separate image validators (report vs progress).
- Very dense one-line code ([ReportProgressService.java](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportProgressService.java), fully qualified annotations in [AdminReportController.java](../publiccare/backend/src/main/java/lk/publiccare/backend/report/AdminReportController.java)).
- Unused `updateReportStatus` client function and demo fixtures in [reportsData.js](../publiccare/src/reportsData.js).
- Outdated documentation (see §7).

### 2.6 Compatibility

Implemented:
- Java 21, Spring Boot 3.5, PostgreSQL, React 19 and Vite 8.
- Backend origin configurable through `VITE_API_BASE_URL`.
- CORS origin configurable through `PUBLICCARE_CORS_ALLOWED_ORIGIN`.

Needs testing:
- Cross-browser behaviour (Chrome, Firefox, Edge, Safari) and mobile widths.
- SameSite=Lax cookies when frontend and backend are on different sites.
- **H2 vs PostgreSQL differences**: the generated `reference` column has to be patched in the H2 test ([ReportDeletionJpaTest.java:44-45](../publiccare/backend/src/test/java/lk/publiccare/backend/report/ReportDeletionJpaTest.java#L44-L45)), so PostgreSQL-specific behaviour should be integration-tested.
- **[I] IPv6 loopback**: `URI.getHost()` returns `[::1]` with brackets, so the `"::1"` check in [AdminSetupRunner.java:48](../publiccare/backend/src/main/java/lk/publiccare/backend/admin/AdminSetupRunner.java#L48) may never match. Verify with a unit test.

### 2.7 Functional correctness

Implemented:
- Status enum enforced in Java and in DB CHECK constraints.
- Reference generated by the database.
- Summary counts computed with the same filter as the list.
- Owner-scoped specifications.

Needs testing:
- Summary totals match the list's `totalElements`.
- Sort and pagination edges.
- Timeline step mapping.
- History order differs by audience: **public is newest-first** (top 10), **citizen and admin are oldest-first**.
- Same-status progress updates create duplicate history rows.
- An empty note string is stored as `""` rather than `null` (the UI always sends `note`, [reportsApi.js:76](../publiccare/src/reportsApi.js#L76)).

---

## TASK 3: User Roles and Permissions

| Capability | Anonymous | Citizen | Administrator | Enforced at |
|---|:-:|:-:|:-:|---|
| View home page and public report feed/detail | ✅ | ✅ | ✅ | `permitAll` GET `/api/public/reports[/*]` |
| Register / log in / get CSRF token | ✅ | ✅ | ✅ | `permitAll` |
| `GET /api/auth/me`, logout | ❌ 401 | ✅ | ✅ | authenticated |
| Create report, upload own image | ❌ 401 (UI disables submit) | ✅ | ✅ **[C]** (admins are authenticated users, so they can own reports too) | `anyRequest().authenticated()` |
| List/view/edit **own** reports, own image, own progress history | ❌ | ✅ (own only) | ✅ (own only, through citizen routes) | owner-scoped query |
| View or edit **another user's** report through citizen routes | ❌ | ❌ 404 | ❌ 404 | owner-scoped query |
| Change report status | ❌ | ❌ 403 | ✅ | `/api/admin/**` |
| View all reports, summary, any image or history | ❌ | ❌ 403 | ✅ | `/api/admin/**` |
| Add progress update (status/note/photo) | ❌ | ❌ 403 | ✅ | `/api/admin/**` |
| Approve/revoke report, update or note for public display | ❌ | ❌ 403 | ✅ | `/api/admin/**` + `adminId()` check |
| Delete report | ❌ | ❌ (no route) | ✅ | `/api/admin/**` |
| Open admin pages in the UI | gate message | gate message | ✅ | `AdminAccessGate` (client only) |
| Create an admin account | ❌ | ❌ | ❌ via web. Only an operator, through the CLI | `AdminSetupApplication` (non-web) |
| Delete user accounts | – | – | – | **Not implemented** |

Evidence: [WebSecurityConfig.java:59-64](../publiccare/backend/src/main/java/lk/publiccare/backend/config/WebSecurityConfig.java#L59-L64), [App.jsx:17-22](../publiccare/src/App.jsx#L17-L22), [SiteHeader.jsx:119-120](../publiccare/src/SiteHeader.jsx#L119-L120).

---

## TASK 4: Business Rules

| ID | Business rule | Evidence | Src |
|---|---|---|---|
| **Authentication** | | | |
| BR-01 | Public registration always creates a `CITIZEN`. A role cannot be self-assigned | [AuthService.java:30](../publiccare/backend/src/main/java/lk/publiccare/backend/auth/AuthService.java#L30) | C+D |
| BR-02 | Emails are unique without regard to case or surrounding spaces (normalised before storage and lookup) | [AuthService.java:24](../publiccare/backend/src/main/java/lk/publiccare/backend/auth/AuthService.java#L24), [DatabaseUserDetailsService.java:21](../publiccare/backend/src/main/java/lk/publiccare/backend/security/DatabaseUserDetailsService.java#L21) | C |
| BR-03 | Password 12–72 characters **and** ≤72 UTF-8 bytes | [AuthDtos.java:16](../publiccare/backend/src/main/java/lk/publiccare/backend/auth/AuthDtos.java#L16), [AuthService.java:25](../publiccare/backend/src/main/java/lk/publiccare/backend/auth/AuthService.java#L25) | C+D |
| BR-04 | Everything except health, public reports and auth entry points requires a session. `/api/admin/**` requires ADMIN | [WebSecurityConfig.java:59-64](../publiccare/backend/src/main/java/lk/publiccare/backend/config/WebSecurityConfig.java#L59-L64) | C+D |
| BR-05 | Every state-changing request must carry a valid CSRF token | [WebSecurityConfig.java:55](../publiccare/backend/src/main/java/lk/publiccare/backend/config/WebSecurityConfig.java#L55) | C+D |
| BR-06 | Only one administrator can be provisioned through setup. A citizen is never promoted | [AdminAccountSetupService.java:30-37](../publiccare/backend/src/main/java/lk/publiccare/backend/admin/AdminAccountSetupService.java#L30-L37) | C+D |
| **Report ownership** | | | |
| BR-07 | Owner = the authenticated user at creation, and it cannot be changed afterwards | [ReportController.java:38](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportController.java#L38), [PublicCareReport.java:49](../publiccare/backend/src/main/java/lk/publiccare/backend/report/PublicCareReport.java#L49) | C+D |
| BR-08 | Citizens can view, edit and read images/history **only** for their own reports. Other users' reports look identical to missing ones (`404`) | [ReportService.java:133-135](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportService.java#L133-L135) | C+D |
| BR-09 | Citizens can edit at any status, including RESOLVED **[C]**. Whether that is intended needs clarification **[I]** | [ReportService.java:91-98](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportService.java#L91-L98) | C / I |
| BR-10 | A user who owns reports cannot be deleted (`ON DELETE RESTRICT`). There is no user-deletion feature | [V2:14](../publiccare/backend/src/main/resources/db/migration/V2__create_reports.sql#L14) | C |
| **Status handling** | | | |
| BR-11 | Allowed statuses are PENDING, IN_PROGRESS, RESOLVED. New reports start as PENDING | [ReportStatus.java](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportStatus.java), [V2:9-10](../publiccare/backend/src/main/resources/db/migration/V2__create_reports.sql#L9-L10) | C+D |
| BR-12 | Only admins change status. The citizen edit DTO has no status field | [ReportDtos.java:15-18](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportDtos.java#L15-L18) | C+D |
| BR-13 | **There are no transition rules**: any status can move to any status (e.g. RESOLVED → PENDING), and a same-status update is accepted and logged **[C]**. Whether this is intended needs clarification **[I]** | [ReportService.java:100-108](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportService.java#L100-L108), [ReportProgressService.java:18-39](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportProgressService.java#L18-L39) | C / I |
| BR-14 | Each progress update is a permanent history record. The report's current status and latest note are overwritten | [ReportProgressService.java:33-36](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportProgressService.java#L33-L36) | C+D |
| **Public approval** | | | |
| BR-15 | Reports, updates and notes are **private by default** | [V5:2](../publiccare/backend/src/main/resources/db/migration/V5__add_public_report_approvals.sql#L2), [V5:16](../publiccare/backend/src/main/resources/db/migration/V5__add_public_report_approvals.sql#L16), [V5:19](../publiccare/backend/src/main/resources/db/migration/V5__add_public_report_approvals.sql#L19) | C |
| BR-16 | There are three independent approval levels. A note is public only when **both** the update and its note are approved, **and** the report itself is approved | [PublicReportService.java:29-37](../publiccare/backend/src/main/java/lk/publiccare/backend/report/PublicReportService.java#L29-L37) | C |
| BR-17 | Report approval is revoked automatically on a status change or a citizen edit. Approvals on individual updates are **not** revoked by these events | [ReportService.java:96](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportService.java#L96), [ReportService.java:104](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportService.java#L104), [ReportProgressService.java:33](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportProgressService.java#L33) | C |
| BR-18 | Approval can be granted at any status (no status precondition) | [ReportService.java:110-117](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportService.java#L110-L117) | C |
| BR-19 | The approver and timestamp are always recorded (DB CHECK constraints) | [V5:7-13](../publiccare/backend/src/main/resources/db/migration/V5__add_public_report_approvals.sql#L7-L13) | C |
| BR-20 | A note can be approved only if it is non-blank | [ReportProgressUpdate.java:35](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportProgressUpdate.java#L35), [V5:40-44](../publiccare/backend/src/main/resources/db/migration/V5__add_public_report_approvals.sql#L40-L44) | C |
| BR-21 | Public pages show at most 24 reports per page and the 10 newest approved updates per report. Images are never public | [PublicReportService.java:12-13](../publiccare/backend/src/main/java/lk/publiccare/backend/report/PublicReportService.java#L12-L13), [ReportControllerTest.java:79](../publiccare/backend/src/test/java/lk/publiccare/backend/report/ReportControllerTest.java#L79) | C |
| BR-22 | **[I]** If the approval request body omits `approved`, it defaults to `false` (Java primitive), so the call **revokes** approval | [PublicReportDtos.java:14](../publiccare/backend/src/main/java/lk/publiccare/backend/report/PublicReportDtos.java#L14) | I |
| **Image upload** | | | |
| BR-23 | Citizen report image: at most one per report, owner only, added after creation. JPG/PNG/WebP, 1 B – 5 MiB, ≤10 MP. Type is detected from content. It cannot be replaced or deleted | [ReportImageService.java:28-49](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportImageService.java#L28-L49), [V3](../publiccare/backend/src/main/resources/db/migration/V3__create_report_images.sql) | C+D |
| BR-24 | Progress photo: optional, one per update, **JPG/PNG only (no WebP)**, ≤5 MiB, ≤10 MP | [ReportProgressService.java:88-98](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportProgressService.java#L88-L98), [V4:12](../publiccare/backend/src/main/resources/db/migration/V4__create_report_progress_updates.sql#L12) | C+D |
| BR-25 | The UI is stricter than the API: citizens can only pick JPG/PNG, while the API also accepts WebP | [CreateIssue.jsx:8](../publiccare/src/CreateIssue.jsx#L8) vs [ReportImageService.java:77](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportImageService.java#L77) | C |
| BR-26 | Images are stored privately under UUID keys and only served through authenticated, owner- or admin-scoped routes | [PrivateImageStorage.java](../publiccare/backend/src/main/java/lk/publiccare/backend/report/PrivateImageStorage.java) | C+D |
| **Deletion** | | | |
| BR-27 | Only admins can delete. Deletion is a permanent hard delete at any status or approval state, and removes the image, history and files | [ReportDeletionService.java:29-65](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportDeletionService.java#L29-L65) | C |
| BR-28 | Citizens cannot delete their own reports | absence in [ReportController.java](../publiccare/backend/src/main/java/lk/publiccare/backend/report/ReportController.java) | C |

---

## TASK 5: Initial Requirement Traceability Matrix

Testing types: **M** = Manual, **UI** = Automated UI, **API** = Automated API, **U** = Unit. The test case IDs refer to Task 6.

| Req ID | Description | Feature | Testing type | Proposed TC IDs | Priority |
|---|---|---|---|---|---|
| FR-01.1 | Register citizen | Registration | M, UI, API, U | MT-01, UI-01, API-02, UT-01 | High |
| FR-01.2 | Registration validation | Registration | UI, API, U | UI-01, API-03, UT-03 | High |
| FR-01.3 | Email/name normalisation | Registration | API, U | API-04, UT-01 | Medium |
| FR-01.4 | Duplicate email 409 | Registration | M, API, U | MT-04, API-04, UT-02 | High |
| FR-01.5 | Role forced to CITIZEN | Registration | API, U | API-02, UT-01 | High |
| FR-01.6 | Confirm password / terms | Registration | M, UI | MT-02, MT-03, UI-01 | Medium |
| FR-01.7 | BCrypt storage | Registration | U | UT-01 | High |
| FR-02.1 | CSRF token and enforcement | Auth | API | API-01, API-05 | High |
| FR-02.2 | Login + session | Auth | M, API | MT-05, API-06 | High |
| FR-02.3 | Generic 401 | Auth | M, UI, API | MT-06, UI-03, API-07 | High |
| FR-02.4 | Login >72-byte password | Auth | API | API-07 | Low |
| FR-02.5 | `/me` | Auth | API | API-08 | High |
| FR-02.6 | Logout | Auth | M, API | MT-07, API-08 | High |
| FR-02.7 | Role-based redirect | Auth | UI | UI-02 | Medium |
| FR-02.8 | Server authorisation matrix | Auth | API | API-09 | High |
| FR-02.9 | Admin route gate | Auth | M, UI | MT-18, UI-13 | Medium |
| FR-03.1 | Create report | Reporting | M, UI, API, U | MT-08, UI-05, API-10, UT-04 | High |
| FR-03.2 | Report field validation | Reporting | UI, API | UI-05, API-11 | High |
| FR-03.3 | Owner from principal | Reporting | API | API-10 | High |
| FR-03.4 | Category list | Reporting | M, API | MT-08, API-11 | Medium |
| FR-03.5 | Single image upload | Reporting | M, API, U | MT-08, API-12, UT-08 | High |
| FR-03.6 | Server image validation | Reporting | API, U | API-12, UT-08 | High |
| FR-03.7 | Client image checks / retry | Reporting | M, UI | MT-09, MT-10, UI-06 | Medium |
| FR-03.8 | Signed-out submit disabled | Reporting | UI | UI-04 | Medium |
| FR-04.1 | My reports list/filter/summary | Citizen mgmt | UI, API, U | UI-07, API-14, UT-04, UT-05 | High |
| FR-04.2 | Own detail / safe 404 | Citizen mgmt | M, API | MT-18, API-15 | High |
| FR-04.3 | Own image retrieval | Citizen mgmt | API | API-13 | Medium |
| FR-04.4 | Edit own report | Citizen mgmt | M, API | MT-11, API-15 | High |
| FR-04.5 | Edit revokes approval | Citizen mgmt | M, API, U | MT-11, API-15, UT-07 | High |
| FR-04.6 | No photo change on edit | Citizen mgmt | M | MT-11 | Low |
| FR-04.7 | Invalid list params | Citizen mgmt | API, U | API-14, UT-04, UT-05 | Medium |
| FR-05.1 | Status timeline | Tracking | UI, U | UI-08, UT-16 | Medium |
| FR-05.2 | Latest admin note | Tracking | M, API | MT-12, API-16 | Medium |
| FR-05.3 | Owner progress history | Tracking | M, API | MT-12, API-17 | High |
| FR-05.4 | Reference displayed | Tracking | M | MT-08 | Medium |
| FR-06.1 | Dashboard | Admin | M, API | MT-12, API-14 | Medium |
| FR-06.2 | Admin list/search | Admin | UI, API | UI-09, API-14 | High |
| FR-06.3 | CSV export | Admin | M, U | MT-12, UT-17 | Low |
| FR-06.4 | Admin detail + image | Admin | API | API-09, API-13 | High |
| FR-06.5 | Progress update atomic | Admin | M, UI, API, U | MT-12, UI-11, API-17, UT-09 | High |
| FR-06.6 | PATCH status | Admin | API, U | API-16, UT-06 | Medium |
| FR-06.7 | Admin history + flags | Admin | API | API-17 | Medium |
| FR-06.8 | Mark as resolved helper | Admin | UI | UI-11 | Low |
| FR-06.9 | First-admin CLI | Admin | U | UT-14, UT-15 | Medium |
| FR-07.1 | Report approval | Public | M, UI, API | MT-13, UI-09, API-18 | High |
| FR-07.2 | Update approval | Public | M, API | MT-13, API-18 | High |
| FR-07.3 | Note approval | Public | M, API, U | MT-13, API-18, UT-10 | High |
| FR-07.4 | Public feed | Public | M, UI, API, U | MT-13, UI-12, API-19, UT-11 | High |
| FR-07.5 | Public detail | Public | UI, API | UI-12, API-19 | High |
| FR-07.6 | Data minimisation | Public | API | API-19 | High |
| FR-07.7 | Auto-revocation | Public | M, API, U | MT-14, API-16, UT-06, UT-07 | High |
| FR-08.1 | Admin delete | Deletion | M, UI, API | MT-15, UI-10, API-20 | High |
| FR-08.2 | After-commit file cleanup | Deletion | U | UT-13 | Medium |
| FR-08.3 | Delete error handling | Deletion | API, U | API-20, UT-13 | Medium |
| FR-08.4 | Delete UI safeguards | Deletion | M, UI | MT-15, UI-10 | Medium |
| FR-08.5 | Citizen cannot delete | Deletion | API | API-20 | High |
| FR-09.1 | Health endpoints | Platform | API | API-21 | Low |
| NFR-SEC | Security characteristics | Cross-cutting | API, U, M | API-05, API-09, API-22, API-23, UT-12, MT-16 | High |
| NFR-USA/COMP | Usability / compatibility | Cross-cutting | M, UI | MT-16, MT-17, UI-14 | Medium |

---

## TASK 6: Test Case Candidates

P = positive, N = negative, B = boundary. All are **proposed and not executed**. Tool suggestions (Playwright/Selenium, REST Assured/Postman, Vitest) are recommendations only; none are installed in the project.

### A. Manual testing

| TC ID | Scenario | Type | Expected result |
|---|---|---|---|
| MT-01 | Register with valid unique details | P | Success message "Your Citizen account has been created…" and the form resets |
| MT-02 | Confirm password differs | N | Browser blocks submit: "Passwords do not match." |
| MT-03 | Terms checkbox unticked | N | Browser required-field validation |
| MT-04 | Register `User@X.com`, then ` user@x.com ` | N | Second attempt shows the duplicate-account message |
| MT-05 | Log in as citizen, then as admin | P | Lands on `/my-reports` and `/admin` respectively |
| MT-06 | Wrong password; unknown email | N | Identical "Invalid email or password." |
| MT-07 | Log out, refresh, press Back | P | Signed-out header. `/my-reports` shows the sign-in prompt |
| MT-08 | Create a report with a valid PNG, then open "View report" | P | Reference `PC-000000NN` shown. Photo, PENDING status and timeline displayed |
| MT-09 | Choose `.gif`; 0-byte `.png`; 5 MiB + 1 B JPG; `.txt` renamed `.jpg` | N/B | Client error for the first three. The renamed file passes the client but the server rejects it (`400`) and the "Retry image upload" path appears |
| MT-10 | Stop the backend after the report is created but before the upload completes | N | "Report … was created, but its image upload failed" plus a Retry button |
| MT-11 | Edit an approved report | P | Saved. Redirected to details. Admin view now shows "Private". No photo edit offered |
| MT-12 | Admin saves IN_PROGRESS + note + JPG; citizen opens the report | P | Citizen sees the new status, latest note and history entry with photo. Dashboard counts update. CSV export opens with the correct columns |
| MT-13 | Approve report → approve update → approve note; then revoke each in turn | P/N | Home feed shows the report, then the update, then the note. Each revoke removes it again |
| MT-14 | Change the status of an approved report | P | Report disappears from the public feed. "Private" shown |
| MT-15 | Delete: cancel the confirm dialog, then accept | N/P | Cancel keeps the row. Accept removes it, and the `/public-reports/{id}` link shows "Report unavailable" |
| MT-16 | Keyboard-only and screen-reader walkthrough (exploratory) | – | All actions reachable. Alerts announced. Escape closes the menu |
| MT-17 | Chrome, Firefox, Edge, Safari and a 375 px mobile width | – | No layout breakage. Cookies work |
| MT-18 | Citizen opens `/admin`, and `/issue-details?id=<another user's report>` | N | "Administrator access required" and "Report unavailable" |
| MT-19 | Backend stopped while using any page | N | "Unable to reach PublicCare…" message, with no crash |

### B. Automated UI testing (e.g. Playwright)

| TC ID | Scenario | Type |
|---|---|---|
| UI-01 | Register form: name `maxLength` 120, password 11 rejected / 12 accepted / 72 accepted, mismatch rejected | B/N/P |
| UI-02 | Login redirect by role (citizen → `/my-reports`, admin → `/admin`) | P |
| UI-03 | Failed login shows `role=alert` with the generic message | N |
| UI-04 | Signed out on `/create-issue`: submit disabled and sign-in link visible | N |
| UI-05 | Create form: each required field empty blocks submit; all valid → success notice with reference | N/P |
| UI-06 | Image picker: invalid type, empty file and >5 MiB messages; preview + Remove photo; input disabled after submit | N/B/P |
| UI-07 | My Reports: status filter and search reset page to 0; counts render; Previous disabled on page 1; Next disabled on the last page | P/B |
| UI-08 | Issue details timeline classes for PENDING / IN_PROGRESS / RESOLVED (use mocked API responses) | P |
| UI-09 | Admin Issues: Approve ↔ Revoke toggle text and the "Approved/Private" label; category, status and sort filters | P |
| UI-10 | Delete: dismissing the dialog keeps the row; accepting it removes the row; buttons disabled while pending | N/P |
| UI-11 | Admin update: note `maxLength` 1000; invalid photo type gives an error; "Mark as resolved" changes only the select; save appends a history item | B/N/P |
| UI-12 | Public section: empty state text; pagination only when `totalPages > 1`; card navigates to `/public-reports/{id}`; unknown ID shows "Report unavailable" | P/N |
| UI-13 | Citizen session on `/admin`, `/admin/issues`, `/admin/update-issue` shows the gate | N |
| UI-14 | Account menu: ArrowDown opens it and focuses the first item; Escape closes it and returns focus to the button | P |

### C. Automated API testing (e.g. REST Assured / Postman + Newman)

| TC ID | Scenario | Type | Expected |
|---|---|---|---|
| API-01 | `GET /api/auth/csrf` | P | `200 {token}` + `XSRF-TOKEN` cookie |
| API-02 | Register valid, including `"role":"ADMIN"` in the body | P/N | `201`, `role=CITIZEN`, no `password`/`passwordHash` fields |
| API-03 | Register boundaries: name 120/121; password 11/12/72/73 chars; 37×"é" (74 bytes); email 254/255; blank and whitespace-only fields | B/N | Valid sizes `201`, invalid sizes `400` |
| API-04 | Duplicate email with different case or padding | N | `409` |
| API-05 | Register, login, logout, POST/PUT/PATCH/DELETE without `X-XSRF-TOKEN` | N | `403` |
| API-06 | Valid login | P | `200`. `JSESSIONID` HttpOnly; SameSite=Lax. Session ID differs from the pre-login ID |
| API-07 | Wrong password vs unknown email; 73-byte password | N/B | Identical `401` bodies; `400` for 73 bytes |
| API-08 | `/me` without a session, with a session, and after logout | P/N | `401` / `200` / `401` |
| API-09 | Every `/api/admin/**` endpoint × {anonymous, citizen, admin} | N/P | `401` / `403` / `2xx` |
| API-10 | Create report (with an `ownerId` of another user in the body) | P/N | `201`, PENDING, reference matches `^PC-\d{8}$`, `ownerId` = session user |
| API-11 | Title 160/161, description 5000/5001, category 80/81, location 240/241, `"   "`, values with surrounding spaces | B/N | `201`/`400`. Stored values trimmed |
| API-12 | Image: valid JPG/PNG/WebP `204`; second upload `409`; GIF `400`; text renamed `.png` `400`; 0 B `400`; valid image at exactly 5,242,880 B `204`; 5,242,881 B `400`/`413`; >10 MP `400`; non-owner `404`; anonymous `401` | P/N/B | as listed |
| API-13 | Image GET headers and scope (owner, non-owner, admin route, no image) | P/N | `no-store`, `nosniff`, correct `Content-Type`; `404` for non-owner or missing |
| API-14 | `/mine` and `/admin/reports`: page −1; size 0/1/100/101; sort `foo`; status `in progress`/`ALL`/`DONE`; `q` matching reference; summary equals `totalElements`; citizen A cannot see B's reports | B/N/P | `400` for invalid input; correct filtering |
| API-15 | GET/PUT another user's report vs a non-existent ID; PUT on an approved report | N/P | Identical `404` bodies. Approval false afterwards; status unchanged |
| API-16 | PATCH status: null / `"DONE"` / note 1000 / note 1001 / same status / new status / null note | N/B/P | `400` for invalid input. Approval kept for the same status and revoked for a change. Null note clears it |
| API-17 | POST progress update: missing status; note 1000/1001; WebP photo; corrupt JPG; valid JPG | N/B/P | `400` for invalid input, **with no history row and no status change**; `201` for valid input; history oldest first |
| API-18 | Approvals: approve/revoke report, update and note; note approval on a blank note; update ID belonging to another report; body `{}` | P/N/B | `400` for blank note; `404` for mismatch; `{}` → `approved:false` (verify BR-22) |
| API-19 | Public: size 0/1/24/25; page −1; unapproved ID; response fields; >10 approved updates; `/api/public/reports/{id}/image` | B/N/P | `400` for bad paging; `404` for unapproved; no owner, reference or admin note fields; 10 newest updates; image route `401` |
| API-20 | Delete: admin `204` then GET `404`; repeat `404`; citizen `403`; no CSRF `403`; ID `abc` `400` | P/N | as listed |
| API-21 | `/api/health`, `/actuator/health` anonymously | P | `200`, status `UP` |
| API-22 | CORS preflight from `http://localhost:5173` vs `http://evil.test` | P/N | Allowed with credentials / rejected |
| API-23 | Trigger a 500 (e.g. upload directory unwritable) | N | No stack trace or exception message in the body |
| API-24 | Two concurrent image uploads to the same report | N | Expected `409`. **Verify the suspected `500` (§2.3)** |

### D. Unit testing (JUnit 5 + Mockito already present; Vitest would need to be added for the frontend)

| TC ID | Unit under test | Scenarios | Existing coverage |
|---|---|---|---|
| UT-01 | `AuthService.register` | Normalisation, BCrypt hash, role forced | Exists ([AuthServiceTest.java:35](../publiccare/backend/src/test/java/lk/publiccare/backend/auth/AuthServiceTest.java#L35)) |
| UT-02 | `AuthService.register` race | `saveAndFlush` throws `DataIntegrityViolationException` → `DuplicateEmailException` | **Gap**: only the pre-check is tested |
| UT-03 | Password byte limit | 72 vs 73 bytes, mixing ASCII and multibyte characters | Partly exists (`é`×37) |
| UT-04 | `ReportService.findPage` | page −1/0, size 0/1/100/101, each allowed sort, invalid sort, direction case | **Gap** |
| UT-05 | `ReportService.criteria` | Status `ALL`, blank, `in progress`, invalid; `q` trimmed and lower-cased | **Gap** |
| UT-06 | `ReportService.updateStatus` | Same status keeps approval; changed status revokes it; null status → exception; null note clears | Partial ([ReportServiceTest.java:102](../publiccare/backend/src/test/java/lk/publiccare/backend/report/ReportServiceTest.java#L102)) |
| UT-07 | `ReportService.updateMine` | Revokes approval; non-owner → `ReportNotFoundException` | Partial |
| UT-08 | `ReportImageService.validateImage/isWebp` | JPG/PNG/WebP signatures, truncated WebP, RIFF length mismatch, VP8/VP8L/VP8X, 10 MP edge, 0 bytes | Partial ([ReportImageServiceTest.java](../publiccare/backend/src/test/java/lk/publiccare/backend/report/ReportImageServiceTest.java)) |
| UT-09 | `ReportProgressService.create` | Note 1000/1001, WebP rejected, storage deleted on save failure, approval revoked on status change | Partial ([ReportProgressServiceTest.java](../publiccare/backend/src/test/java/lk/publiccare/backend/report/ReportProgressServiceTest.java)) |
| UT-10 | `ReportProgressUpdate.approveNoteForPublic` | `null`, `""`, `"   "` → exception; valid note → flags and actor set | **Gap** (direct) |
| UT-11 | `PublicReportService` | Note masked when not approved; size 24/25; unapproved detail → not found | Partial ([PublicReportServiceTest.java](../publiccare/backend/src/test/java/lk/publiccare/backend/report/PublicReportServiceTest.java)) |
| UT-12 | `PrivateImageStorage.resolve` | `../x.jpg`, `abc.jpg`, `.gif` extension, valid UUID key | **Gap** |
| UT-13 | `ReportDeletionService` | After-commit cleanup, missing report, DB failure, file-delete failure logged | Exists ([ReportDeletionServiceTest.java](../publiccare/backend/src/test/java/lk/publiccare/backend/report/ReportDeletionServiceTest.java)) |
| UT-14 | `AdminSetupRunner.isLoopbackPostgres` (private; needs refactor or reflection) | `localhost`, `127.0.0.1`, `[::1]`, remote host, `jdbc:mysql:` | **Gap**. Verifies the IPv6 concern |
| UT-15 | `AdminAccountSetupService` | Missing fields, re-run idempotency, refuse citizen, refuse second admin | Exists |
| UT-16 | `reportsData.getStatusProgress` / `normalizeStatus`, `reportsApi.queryString`, `reportStatusLabel` (frontend) | Each status; `"in_progress"`, `"IN-PROGRESS"`, unknown; empty/null params dropped | **Gap** (needs Vitest) |
| UT-17 | `downloadCsv` escaping (frontend, would need to be exported) | Values containing `"`, commas, newlines | **Gap** |

---

## 7. Observations and documentation gaps

**Documentation that contradicts the code** (so tests should follow the code, and the docs should be updated):
1. [AUTH_LOCAL_VERIFICATION.md:26](../publiccare/AUTH_LOCAL_VERIFICATION.md#L26) says login goes to `/my-reports`. The code sends admins to `/admin` ([AccountPage.jsx:32](../publiccare/src/AccountPage.jsx#L32)).
2. [AUTH_LOCAL_VERIFICATION.md:38](../publiccare/AUTH_LOCAL_VERIFICATION.md#L38) says there is no admin issue controller. `AdminReportController` exists at `/api/admin/reports`.
3. [AUTH_LOCAL_VERIFICATION.md:46](../publiccare/AUTH_LOCAL_VERIFICATION.md#L46) says issue submission and admin operations are browser-local demos. They use the backend API, and [README.md:12](../publiccare/README.md#L12) agrees with the code.
4. [backend/README.md](../publiccare/backend/README.md) does not document `DELETE /api/admin/reports/{id}`, the three public-approval endpoints, or `/api/public/reports`. These requirements are confirmed from code only.
5. The README documents a category filter for `/mine` (API: confirmed). The My Reports UI does not expose it.

**Inferred items to clarify with the team or lecturer before writing expected results:**
- BR-09: citizens can edit RESOLVED reports.
- BR-13: there are no status transition rules.
- BR-22: an empty approval body revokes approval.
- §2.3: a concurrent duplicate image upload probably returns 500.
- §2.6: the IPv6 loopback check.
- §2.4: the edit-page category fallback.
- §2.1: no login throttling.

All of these come from reading the code, and each still needs to be confirmed by running the related test.

**Summary counts:** 59 functional requirements in 9 groups (all confirmed in code), 28 business rules (25 confirmed, 3 inferred or needing clarification), and 74 proposed test cases: 19 manual, 14 UI, 24 API and 17 unit.

---

## Editor's notes (added when archiving; the report above was not changed)

These consistency points were found by counting the IDs in the report when it was saved. They are recorded here instead of being corrected in the text, so that the original findings stay as written. The group should resolve them before submission.

1. **Functional requirement count.** The closing summary says "59 functional requirements in 9 groups". The TASK 1 tables list **58** sub-requirements: FR-01 (7), FR-02 (9), FR-03 (8), FR-04 (7), FR-05 (4), FR-06 (9), FR-07 (7), FR-08 (5), FR-09 (2).
2. **FR-09.2 is not in the RTM.** FR-09.2 (Flyway-managed schema) is defined in TASK 1 but has no row in the TASK 5 traceability matrix.
3. **Two test cases are not traced in the RTM.** MT-19 (backend stopped while using any page) and API-24 (concurrent image uploads to the same report) are defined in TASK 6 but are not referenced in TASK 5. API-24 relates to the inferred §2.3 reliability item.

The other counts match the report: 28 business rules (25 confirmed, 3 inferred or needing clarification), and 74 proposed test cases (19 manual, 14 UI, 24 API, 17 unit). Every test case ID referenced in the RTM is defined in TASK 6.

**Reminder:** none of the proposed test cases have been executed.
