# PublicCare QA case study · Sections 3.7–3.10

**Member 4 · S. A. Chathura Madushan · 24 November 2026**  
System under test: the Member 4 handover at b97bd96 on chathura. The registration retest and final static analysis include fix f78deb1. Existing Part 01 requirements anchor the checks. [Reproduction commands](member4/REPRODUCE.md) · [Evidence directory](../evidence/member4/)

## 3.7 Performance, security and usability

### HTTP performance

Five JMeter users read the Spring Boot health endpoint on loopback. The two-second ramp-up, 20 iterations per user, 250 ms pacing and 10-second safety cap model a small group of periodic readers. A readiness request preceded measurement. Each sample checked the health payload; the JTL records the HTTP status separately. Linux x86-64, Java 21, Spring Boot 3.5.16, H2 in PostgreSQL compatibility mode. Client and server ran in the same isolated environment.

| Completed requests | HTTP 200 / assertion success | Mean | Median | p95 | Maximum | JMeter throughput |
|---:|---:|---:|---:|---:|---:|---:|
| 100 | 100/100 | 9.37 ms | 8 ms | 17 ms | 55 ms | 14.6 requests/s |

The minimum was 3 ms. The p95 uses the 95th ordered elapsed time. Throughput comes from JMeter’s final summary interval. The endpoint returns a constant status map and performs no report lookup. This establishes HTTP-stack behavior under light load. Database-backed listings, internet latency, concurrent writes and saturation remain unmeasured. Part 01 leaves the response-time target open (OQ-4), so this run establishes a baseline without an SLA verdict. [Plan](member4/health.jmx) · [100 samples](../evidence/member4/health.jtl) · [Console](../evidence/member4/jmeter-console.txt)

### Manual OWASP control checks

On a fresh local service with no cookies, send one ordinary GET to each path. FR-02 restricts session information and FR-06 restricts administration. Both checks address OWASP Broken Access Control. The public health request serves as a positive control.

| Check | Expected | Observed | Assessment |
|---|---|---|---|
| Public GET /api/health | 200 and UP | 200 and UP | Service reachable |
| Anonymous GET /api/admin/reports | 401 with no report data | 401, authentication-required JSON | Passed for anonymous access |
| Anonymous GET /api/auth/me | 401 with no account data | 401, authentication-required JSON | Passed for anonymous access |

The administrative route requires ADMIN in WebSecurityConfig; remaining protected routes require an authenticated principal. The existing MockMvc suite separately exercised citizen/admin authorization and CSRF requirements. No defect severity was assigned to these passes. Retain both negative checks when changing route permissions. Results cover these routes and identities only. [HTTP responses](../evidence/member4/access-checks.txt) · [Suite results](../evidence/member4/junit-summary.txt)

### Usability walkthrough

| User task | Method and result |
|---|---|
| Recover from a registration typo (FR-01) | At 1100 × 900, submit two different passwords, then correct only the original field. The form incorrectly kept its mismatch message. The same path passed after the fix. |
| Find the next action while signed out (FR-03.8) | At 390 × 844, the submit button was disabled and the nearby sign-in link opened login. The session response was a synthetic 401 fixture. |
| Enter login details using the keyboard (FR-02) | Tab moved from email to password. Show and Hide changed the password field in both directions. The login page fit the viewport without horizontal scrolling. |

These were scripted task walkthroughs in Chromium 153.0.8010.0. No participant satisfaction scores or real-citizen task times were collected. [Mobile results](../evidence/member4/mobile.txt) · [Registration before](../evidence/member4/registration-before.png) · [Registration after](../evidence/member4/registration-after.png)

## 3.8 Defect tracking and root cause

| Field | PC-QA-01 |
|---|---|
| Record | [GitHub issue #1](https://github.com/madushankachamara891-pixel/PublicCare-QA-Member4-Chathura-/issues/1) |
| Severity / priority | Medium / P2. A citizen can recover by editing confirmation again, but the page does not explain this workaround. |
| Preconditions and steps | Start on /register, fill the required fields, accept the terms, submit different valid-length passwords, then edit only the first password to match and submit again. |
| Expected / actual | Matching passwords should permit submission. The browser showed “Passwords do not match.” and suppressed the submit event. |
| Root cause | The submit handler sets custom validity on the confirmation input. Only that field cleared its error on edit. Native constraint validation blocked the next submit before React could recompute the comparison. |
| Lifecycle | New in #1 → assigned to Chathura124 → fixed in f78deb1 → verified with the same browser assertion → closed for the Member 4 copy. |
| Verification | Before: valuesMatch=true, submitted=false. After: valuesMatch=true, submitted=true with an empty validation message. Both runs first checked that an actual mismatch remained invalid. |

The automated check stops at the submit event; no account was created. [Regression test](member4/registration.cjs) · [Before output](../evidence/member4/registration-before.txt) · [After output](../evidence/member4/registration-after.txt)

## 3.9 Quality metrics and SonarQube

The backend suite ran 72 tests with zero failures, errors or skips. The Vite production build and source-only ESLint check passed. Running repository-wide ESLint after generating JaCoCo HTML also linted generated JavaScript and returned 138 errors in report assets. The source-only check isolates product code.

| Metric | Measured result | Interpretation |
|---|---:|---|
| SonarQube coverage | 36.0% | Frontend and backend scope; only backend JaCoCo data was imported. |
| SonarQube bugs | 3 | Static rule findings requiring triage. |
| SonarQube vulnerabilities | 0 | Count reported by the enabled rules. |
| Code smells | 88 | Maintainability findings from the active quality profiles. |
| Technical debt | 527 | Minutes of estimated remediation effort. |
| Duplicated lines | 0.0% | Percentage of analyzed lines. |
| Analyzed code lines | 4206 | SonarQube non-comment lines across the configured source roots. |
| Backend line coverage | 67.55% | JaCoCo measured 433 covered out of 641. |
| Backend branch coverage | 41.72% | JaCoCo measured 126 covered out of 302. |
| Observed defect density | 0.24 per KLOC | One manually confirmed product defect during this exercise divided by 4.206 analyzed KLOC. This measures discovery in a small sample. |

The Sonar way quality gate passed for this baseline. A gate pass can coexist with existing findings. No frontend browser coverage was instrumented. The scanner reported unresolved Java types and lacked Git blame metadata in the isolated checkout. Scope includes project/src and backend production Java; database migrations and infrastructure were outside this scan.

**Improvements grounded in the findings**

- ReportImageService.java (line 101, java:S3776): Break the flagged method into named validation steps, preserving behavior with focused tests.
- ReportImageService.java (line 83, java:S1192): Centralize repeated validation text so all rejected inputs receive consistent feedback.
- PublicReportService.java (line 13, java:S1068): Review the unused field against the intended public-report limits, then remove it or enforce the intended limit with a test.

[Scanner measurements](../evidence/member4/sonar-measures.json) · [Rule findings](../evidence/member4/sonar-issues.json) · [Scanner outcome](../evidence/member4/sonar-run.txt) · [JaCoCo class data](../evidence/member4/jacoco.csv)

## 3.10 Case study and best practices

PublicCare lets citizens submit local issues and administrators manage them. The handover already documented functional requirements and had controller and service tests. Our additional question was whether a citizen could recover from a simple input mistake and whether protected routes, the HTTP stack and the codebase behaved as the team expected.

The 72 passing backend tests and the failed registration walkthrough described different layers of the same product. The browser prevented a valid form from reaching the API, so no controller assertion could observe the defect. Repeating the same task before and after one change produced a verifiable issue lifecycle. For the next sprint, attach this regression to the FR-01 acceptance criteria and run it alongside the existing API checks on pull requests.

For DevOps, retain the JTL, JaCoCo export, scanner results and tested revision with each comparable build. Evaluate trends on a stable runner. A follow-up performance experiment should use a controlled PostgreSQL fixture and a public report listing at realistic page sizes. Agree the latency target with the group before assigning pass/fail to that experiment. The current run supports a local health baseline, a verified registration fix and prioritized maintenance work. Participant usability, production capacity and a group-wide release decision require additional evidence.
